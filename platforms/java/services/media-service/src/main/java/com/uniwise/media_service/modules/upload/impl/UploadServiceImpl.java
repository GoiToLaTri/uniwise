package com.uniwise.media_service.modules.upload.impl;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.uniwise.common.dto.response.UploadResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.ServerError;
import com.uniwise.common.exception.errors.ValidationError;
import com.uniwise.media_service.modules.upload.UploadService;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadServiceImpl implements UploadService {

    MinioClient minioClient;

    @NonFinal
    @Value("${minio.bucket-name}")
    String bucketName;

    @NonFinal
    @Value("${minio.public-url}")
    String publicUrl;

    @Override
    public UploadResponse uploadThumbnail(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("Uploaded file is null or empty");
            throw new HttpException(ValidationError.INVALID_REQUEST_BODY);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate a unique filename using UUID
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        String objectKey = "thumbnails/" + uniqueFilename;
        
        log.info("Uploading thumbnail {} to MinIO bucket {} as {}", originalFilename, bucketName, objectKey);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
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
}
