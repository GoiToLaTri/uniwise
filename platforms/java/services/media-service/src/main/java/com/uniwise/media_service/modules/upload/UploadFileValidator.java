package com.uniwise.media_service.modules.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.uniwise.common.exception.HttpException;
import com.uniwise.media_service.configuration.MediaUploadProperties;
import com.uniwise.media_service.modules.upload.error.UploadError;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Validates untrusted multipart files before they are uploaded to object
 * storage.
 *
 * <p>
 * Validation combines the configured size limit with the filename extension,
 * client-declared MIME type, and the file's leading signature bytes. An
 * extension or MIME type alone is not trusted because both values are supplied
 * by the client.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadFileValidator {
    // Enough bytes to identify every file format currently allowed by the service.
    static final int SIGNATURE_LENGTH = 16;

    // JPEG files start with the Start Of Image marker: FF D8 FF.
    static final FileType JPEG = new FileType(
            ".jpg",
            "image/jpeg",
            header -> startsWith(header, 0xFF, 0xD8, 0xFF));
    // PNG uses a fixed eight-byte signature at the beginning of the file.
    static final FileType PNG = new FileType(
            ".png",
            "image/png",
            header -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A));
    // WebP is a RIFF container whose bytes 8-11 must contain the WEBP marker.
    static final FileType WEBP = new FileType(
            ".webp",
            "image/webp",
            header -> startsWith(header, 0x52, 0x49, 0x46, 0x46)
                    && startsWithAt(header, 8, 0x57, 0x45, 0x42, 0x50));
    // ISO Base Media files such as MP4 contain an ftyp box at byte offset 4.
    static final FileType MP4 = new FileType(
            ".mp4",
            "video/mp4",
            header -> startsWithAt(header, 4, 0x66, 0x74, 0x79, 0x70));

    // Both .jpg and .jpeg are accepted, but stored objects use canonical .jpg.
    static final Map<String, FileType> THUMBNAIL_TYPES = Map.of(
            "jpg", JPEG,
            "jpeg", JPEG,
            "png", PNG,
            "webp", WEBP);
    // The existing video-processing pipeline currently supports MP4 only.
    static final Map<String, FileType> VIDEO_TYPES = Map.of("mp4", MP4);

    MediaUploadProperties properties;

    /**
     * Validates a thumbnail against the configured thumbnail size limit and
     * supported image formats.
     *
     * @param file multipart file received from the client
     * @return canonical metadata that is safe to use for object storage
     * @throws HttpException when the file fails any validation rule
     */
    public ValidatedUploadFile validateThumbnail(MultipartFile file) {
        return validate(file, properties.getThumbnailMaxSize().toBytes(), THUMBNAIL_TYPES);
    }

    /**
     * Validates a video against the configured video size limit and supported
     * video formats.
     *
     * @param file multipart file received from the client
     * @return canonical metadata that is safe to use for object storage and events
     * @throws HttpException when the file fails any validation rule
     */
    public ValidatedUploadFile validateVideo(MultipartFile file) {
        return validate(file, properties.getVideoMaxSize().toBytes(), VIDEO_TYPES);
    }

    private ValidatedUploadFile validate(
            MultipartFile file,
            long maxSize,
            Map<String, FileType> allowedTypes) {
        // Fail before opening a stream or contacting external storage.
        if (file == null || file.isEmpty())
            throw new HttpException(UploadError.FILE_REQUIRED);
        if (file.getSize() > maxSize)
            throw new HttpException(UploadError.FILE_TOO_LARGE);

        // Resolve the expected type from the normalized filename extension.
        String extension = getExtension(file.getOriginalFilename());
        FileType expectedType = allowedTypes.get(extension);
        if (expectedType == null)
            throw new HttpException(UploadError.FILE_NAME_INVALID);

        // Ensure the client-declared MIME type agrees with the extension.
        String declaredContentType = normalizeContentType(file.getContentType());
        if (!expectedType.contentType().equals(declaredContentType))
            throw new HttpException(UploadError.FILE_TYPE_NOT_ALLOWED);

        // Finally inspect the actual leading bytes instead of trusting metadata.
        byte[] signature = readSignature(file);
        if (!expectedType.signatureMatcher().matches(signature))
            throw new HttpException(UploadError.FILE_SIGNATURE_INVALID);

        // Downstream code receives server-controlled metadata, not client input.
        return new ValidatedUploadFile(expectedType.canonicalExtension(), expectedType.contentType());
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename))
            throw new HttpException(UploadError.FILE_NAME_INVALID);

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension))
            throw new HttpException(UploadError.FILE_NAME_INVALID);
        return extension.toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType))
            throw new HttpException(UploadError.FILE_TYPE_NOT_ALLOWED);
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] readSignature(MultipartFile file) {
        // Open a short-lived stream so the upload service can open a fresh stream later.
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(SIGNATURE_LENGTH);
        } catch (IOException e) {
            throw new HttpException(UploadError.FILE_UNREADABLE);
        }
    }

    private static boolean startsWith(byte[] source, int... expected) {
        return startsWithAt(source, 0, expected);
    }

    private static boolean startsWithAt(byte[] source, int offset, int... expected) {
        if (source.length < offset + expected.length)
            return false;
        for (int index = 0; index < expected.length; index++) {
            if (Byte.toUnsignedInt(source[offset + index]) != expected[index])
                return false;
        }
        return true;
    }

    record FileType(
            String canonicalExtension,
            String contentType,
            SignatureMatcher signatureMatcher) {
    }

    // Allows each supported format to define its own signature check.
    @FunctionalInterface
    interface SignatureMatcher {
        boolean matches(byte[] header);
    }
}
