package com.uniwise.media_service.modules.upload.impl;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.uniwise.common.dto.response.UploadResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.ServerError;
import com.uniwise.media_service.configuration.MinioProperties;
import com.uniwise.media_service.modules.upload.LessonUploadAuthorizer;
import com.uniwise.media_service.modules.upload.UploadFileValidator;
import com.uniwise.media_service.modules.upload.UploadService;
import com.uniwise.media_service.modules.upload.ValidatedUploadFile;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadServiceImpl implements UploadService {

    MinioClient minioClient;
    MinioProperties minioProperties;
    EventPublisher eventPublisher;
    UploadFileValidator uploadFileValidator;
    LessonUploadAuthorizer lessonUploadAuthorizer;

    @Override
    public UploadResponse uploadThumbnail(MultipartFile file) {
        ValidatedUploadFile validatedFile = uploadFileValidator.validateThumbnail(file);

        String bucketName = minioProperties.getBucketName();
        String publicUrl = minioProperties.getPublicUrl();

        String originalFilename = file.getOriginalFilename();

        // Generate a unique filename using UUID
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + validatedFile.extension();
        String objectKey = "thumbnails/" + uniqueFilename;
        
        log.info("Uploading thumbnail {} to MinIO bucket {} as {}", originalFilename, bucketName, objectKey);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(validatedFile.contentType())
                            .build()
            );

            // Construct public access URL
            String fileUrl = String.format("%s/%s/%s", publicUrl, bucketName, objectKey);
            log.info("Successfully uploaded thumbnail. Public URL: {}", fileUrl);

            return UploadResponse.builder()
                    .url(fileUrl)
                    .fileName(uniqueFilename)
                    .build();
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while uploading file to MinIO", e);
            throw new HttpException(ServerError.SERVER_ERROR);
        }
    }

    @Override
    public UploadResponse uploadVideo(MultipartFile file, String lessonId) {
        String authorizedLessonId = lessonUploadAuthorizer.authorize(lessonId);
        ValidatedUploadFile validatedFile = uploadFileValidator.validateVideo(file);

        String bucketName = minioProperties.getBucketName();
        String publicUrl = minioProperties.getPublicUrl();

        String originalFilename = file.getOriginalFilename();

        // Generate a unique filename using UUID
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + validatedFile.extension();
        String objectKey = "tmp/" + uniqueFilename;

        log.info("Uploading video {} to MinIO bucket {} as {}", originalFilename, bucketName, objectKey);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(validatedFile.contentType())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to upload video to MinIO bucket {} as {}", bucketName, objectKey, e);
            throw new HttpException(ServerError.SERVER_ERROR);
        }

        // Construct file URL
        String fileUrl = String.format("%s/%s/%s", publicUrl, bucketName, objectKey);
        log.info("Successfully uploaded video. URL: {}", fileUrl);

        // Publish event to RabbitMQ
        VideoUploadedEvent event = VideoUploadedEvent.builder()
                .lessonId(authorizedLessonId)
                .objectKey(objectKey)
                .bucketName(bucketName)
                .originalFilename(originalFilename)
                .contentType(validatedFile.contentType())
                .size(file.getSize())
                .build();

        try {
            log.info("Publishing VideoUploadedEvent for {}", uniqueFilename);
            eventPublisher.publish(Exchanges.MEDIA, RoutingKeys.VIDEO_UPLOADED, event);
        } catch (Exception e) {
            log.error(
                    "Failed to publish VideoUploadedEvent for MinIO object {}/{}. Attempting compensating cleanup",
                    bucketName,
                    objectKey,
                    e);
            removeUploadedObject(bucketName, objectKey);
            throw new HttpException(ServerError.SERVER_ERROR);
        }

        return UploadResponse.builder()
                .url(fileUrl)
                .fileName(uniqueFilename)
                .build();
    }

    private void removeUploadedObject(String bucketName, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
            log.warn("Removed MinIO object {}/{} after event publication failure", bucketName, objectKey);
        } catch (Exception cleanupError) {
            log.error(
                    "Failed to remove MinIO object {}/{} after event publication failure; manual cleanup is required",
                    bucketName,
                    objectKey,
                    cleanupError);
        }
    }
}
