package com.uniwise.media_service.configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO client with endpoint: {}", minioProperties.getEndpoint());
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean
    public ApplicationRunner initializeMinio(MinioClient minioClient) {
        return args -> {
            try {
                log.info("Checking if MinIO bucket '{}' exists...", minioProperties.getBucketName());
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
                    log.info("Successfully created MinIO bucket: {}", minioProperties.getBucketName());
                } else {
                    log.info("MinIO bucket '{}' already exists.", minioProperties.getBucketName());
                }
            } catch (Exception e) {
                log.error("Failed to initialize MinIO bucket '{}': {}", minioProperties.getBucketName(), e.getMessage(), e);
            }
        };
    }
}
