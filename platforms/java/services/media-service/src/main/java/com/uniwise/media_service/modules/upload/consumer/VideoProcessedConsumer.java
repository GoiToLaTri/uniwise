package com.uniwise.media_service.modules.upload.consumer;

import com.uniwise.media_service.configuration.MinioProperties;
import com.uniwise.media_service.configuration.RabbitMQConfig;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoProcessedEvent;
import com.uniwise.platform_event_contract.event.media.VideoTranscodedEvent;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_starter.publisher.EventPublisher;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoProcessedConsumer {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final EventPublisher eventPublisher;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSED_QUEUE)
    public void handleVideoProcessedEvent(EventEnvelope<VideoProcessedEvent> envelope) {
        VideoProcessedEvent event = envelope.getPayload();
        String lessonId = event.getLessonId();
        String status = event.getStatus();

        log.info("Received VideoProcessedEvent: lessonId={}, status={}", lessonId, status);

        if (isInvalidLessonId(lessonId)) {
            log.error("Invalid lessonId received in VideoProcessedEvent. Aborting process.");
            return;
        }

        if (!"SUCCESS".equalsIgnoreCase(status)) {
            log.warn("Video processing failed for lessonId={}. Relocation aborted. Publishing FAILED event.", lessonId);
            publishTranscodedEvent(lessonId, null, null, "FAILED");
            return;
        }

        try {
            relocateVideoFiles(lessonId);
        } catch (Exception e) {
            log.error("Failed to relocate transcoded files and notify course-service for lessonId={}", lessonId, e);
            // Rethrow Exception to notify RabbitMQ to retry
            throw new RuntimeException(
                    "Failed to relocate transcoded files and notify course-service for lessonId=" + lessonId, e);
        }
    }

    private boolean isInvalidLessonId(String lessonId) {
        return lessonId == null || lessonId.trim().isEmpty();
    }

    private void relocateVideoFiles(String lessonId) throws Exception {
        String bucketName = minioProperties.getBucketName();
        String processedPrefix = "processed/" + lessonId + "/";
        String lessonPrefix = "lessons/" + lessonId + "/";

        log.info("Starting relocation of transcoded files from {} to {} in bucket {}",
                processedPrefix, lessonPrefix, bucketName);

        // List objects in processed/{lessonId}/
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(processedPrefix)
                        .recursive(true)
                        .build());

        // Obtain iterator and consume it once to prevent multiple HTTP list requests to MinIO
        java.util.Iterator<Result<Item>> iterator = results.iterator();
        boolean hasSourceFiles = iterator.hasNext();
        if (hasSourceFiles) {
            clearDestinationFolder(bucketName, lessonPrefix);
        }

        boolean movedAny = moveFiles(iterator, bucketName, processedPrefix, lessonPrefix);

        if (movedAny) {
            writeCompletionMarker(bucketName, lessonPrefix);
        } else {
            boolean alreadyRelocated = checkAlreadyRelocated(bucketName, lessonPrefix);
            if (alreadyRelocated) {
                log.info("Relocation already completed in a previous attempt for lessonId={}. Publishing VideoTranscodedEvent.", lessonId);
            } else {
                log.warn("No transcoded HLS files found in {} to relocate, and .completed marker not found at destination.", processedPrefix);
                return;
            }
        }

        log.info("Relocation completed for lessonId={}. Publishing VideoTranscodedEvent.", lessonId);
        publishTranscodedEvent(lessonId, bucketName, lessonPrefix, "SUCCESS");
    }

    private void clearDestinationFolder(String bucketName, String lessonPrefix) throws Exception {
        log.info("New source HLS files detected. Clearing old files at destination: {}", lessonPrefix);
        Iterable<Result<Item>> oldItems = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(lessonPrefix)
                        .recursive(true)
                        .build());
        
        for (Result<Item> result : oldItems) {
            Item item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(item.objectName())
                            .build());
        }
    }

    private boolean moveFiles(java.util.Iterator<Result<Item>> iterator, String bucketName, String processedPrefix, String lessonPrefix) throws Exception {
        boolean movedAny = false;
        while (iterator.hasNext()) {
            Result<Item> result = iterator.next();
            Item item = result.get();
            // Skip if it is a directory
            if (item.isDir()) {
                continue;
            }
            String srcKey = item.objectName();
            String fileName = srcKey.substring(processedPrefix.length());
            String destKey = lessonPrefix + fileName;

            log.info("Moving MinIO object: {} -> {}", srcKey, destKey);

            // Copy to new destination
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(destKey)
                            .source(CopySource.builder().bucket(bucketName).object(srcKey).build())
                            .build());

            // Remove original object
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(srcKey)
                            .build());
            movedAny = true;
        }
        return movedAny;
    }

    private void writeCompletionMarker(String bucketName, String lessonPrefix) throws Exception {
        String markerKey = lessonPrefix + ".completed";
        log.info("Writing relocation completion marker: {}", markerKey);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(markerKey)
                        .stream(new java.io.ByteArrayInputStream(new byte[0]), 0, -1)
                        .contentType("application/octet-stream")
                        .build());
    }

    private boolean checkAlreadyRelocated(String bucketName, String lessonPrefix) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(lessonPrefix + ".completed")
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void publishTranscodedEvent(String lessonId, String bucketName, String lessonPrefix, String status) {
        String videoUrl = null;
        if ("SUCCESS".equalsIgnoreCase(status)) {
            String publicUrl = minioProperties.getPublicUrl();
            videoUrl = String.format("%s/%s/%splaylist.m3u8", publicUrl, bucketName, lessonPrefix);
        }

        VideoTranscodedEvent transcodedEvent = VideoTranscodedEvent.builder()
                .lessonId(lessonId)
                .videoUrl(videoUrl)
                .status(status)
                .build();

        eventPublisher.publish(Exchanges.MEDIA, RoutingKeys.VIDEO_TRANSCODED, transcodedEvent);
        log.info("Published VideoTranscodedEvent for lessonId: {} with status: {} and URL: {}", lessonId, status, videoUrl);
    }
}
