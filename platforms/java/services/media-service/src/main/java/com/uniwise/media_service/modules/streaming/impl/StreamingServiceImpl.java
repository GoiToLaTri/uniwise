package com.uniwise.media_service.modules.streaming.impl;

import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.ServerError;
import com.uniwise.common.exception.errors.ValidationError;
import com.uniwise.media_service.configuration.MinioProperties;
import com.uniwise.media_service.modules.streaming.StreamingService;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StreamingServiceImpl implements StreamingService {

    MinioClient minioClient;
    MinioProperties minioProperties;

    @Override
    public ResponseEntity<Resource> streamVideo(String lessonId, String filename) {
        if (lessonId == null || lessonId.isBlank() || filename == null || filename.isBlank()) {
            throw new HttpException(ValidationError.INVALID_REQUEST_BODY);
        }

        String bucketName = minioProperties.getBucketName();
        String objectKey = "lessons/" + lessonId + "/" + filename;

        try {
            // Get object stat to know its size
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            // Get object stream
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            InputStreamResource resource = new InputStreamResource(stream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(stat.size());
            
            // Tự động support Accept-Ranges (nếu HttpMessageConverter hỗ trợ)
            
            // Set Content-Type based on extension
            if (filename.endsWith(".m3u8")) {
                headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
            } else if (filename.endsWith(".ts")) {
                headers.setContentType(MediaType.parseMediaType("video/MP2T"));
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (io.minio.errors.ErrorResponseException e) {
            log.error("MinIO Error while streaming object {}", objectKey, e);
            throw new HttpException(ServerError.SERVER_ERROR);
        } catch (Exception e) {
            log.error("Unexpected error while streaming object {}", objectKey, e);
            throw new HttpException(ServerError.SERVER_ERROR);
        }
    }
}
