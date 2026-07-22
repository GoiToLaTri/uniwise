package com.uniwise.ffmpeg_worker.service.impl;

import com.uniwise.ffmpeg_worker.service.FfmpegService;
import com.uniwise.ffmpeg_worker.service.VideoTranscodeService;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;

import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.uniwise.platform_event_starter.publisher.EventPublisher;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.media.VideoProcessedEvent;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoTranscodeServiceImpl implements VideoTranscodeService {

    private final MinioClient minioClient;
    private final FfmpegService ffmpegService;
    private final EventPublisher eventPublisher;

    @Override
    public void transcodeVideoToHls(EventEnvelope<VideoUploadedEvent> envelope) {
        VideoUploadedEvent event = envelope.getPayload();
        String lessonId = event.getLessonId();
        
        log.info("Start processing HLS video transcoding for lessonId={}", lessonId);

        String fileExtension = getFileExtension(event.getOriginalFilename());
        String inputFileName = "input_" + lessonId + fileExtension;
        String hlsFolderName = "hls_" + lessonId;

        File localInputFile = ffmpegService.getLocalTempFile(inputFileName);
        File localHlsFolder = ffmpegService.getLocalTempFile(hlsFolderName);

        boolean transcodeSuccess = false;
        Long durationMillis = null;

        try {
            // Ensure HLS folder exists on Host
            if (!localHlsFolder.exists()) {
                boolean created = localHlsFolder.mkdirs();
                log.info("Created local HLS folder: {}, success={}", localHlsFolder.getAbsolutePath(), created);
            }

            // Step 1: Download raw video from MinIO to host temp path
            log.info("Downloading object '{}' from bucket '{}' to local path: {}", 
                    event.getObjectKey(), event.getBucketName(), localInputFile.getAbsolutePath());
            
            minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                            .bucket(event.getBucketName())
                            .object(event.getObjectKey())
                            .filename(localInputFile.getAbsolutePath())
                            .build()
            );

            // Step 2: Map paths for container execution
            String containerInputPath = ffmpegService.getContainerFilePath(inputFileName);
            String containerHlsFolder = ffmpegService.getContainerFilePath(hlsFolderName);

            // Step 3: Read and validate the source video duration
            durationMillis = ffmpegService.probeDurationMillis(containerInputPath);
            log.info("Probed video duration for lessonId={}: {} ms", lessonId, durationMillis);

            // Step 4: Run ffmpeg command to convert to HLS format
            // Generates playlist.m3u8 and segment_xxx.ts files inside containerHlsFolder
            List<String> ffmpegArgs = List.of(
                    "-y",
                    "-i", containerInputPath,
                    "-codec:v", "libx264",
                    "-crf", "23",
                    "-preset", "medium",
                    "-codec:a", "aac",
                    "-b:a", "128k",
                    "-hls_time", "10",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", containerHlsFolder + "/segment_%03d.ts",
                    containerHlsFolder + "/playlist.m3u8"
            );

            log.info("Running HLS conversion in container: {} -> {}", containerInputPath, containerHlsFolder);
            boolean success = ffmpegService.execute(ffmpegArgs);

            if (!success) {
                log.error("FFmpeg HLS conversion failed for lessonId={}. Aborting upload.", lessonId);
                publishProcessedEvent(lessonId, event.getBucketName(), "FAILED", null);
                return;
            }

            // Step 5: Scan and upload all files from the local HLS folder to MinIO
            File[] hlsFiles = localHlsFolder.listFiles();
            if (hlsFiles != null && hlsFiles.length > 0) {
                log.info("Found {} HLS files to upload.", hlsFiles.length);
                for (File file : hlsFiles) {
                    String outputObjectKey = "processed/" + lessonId + "/" + file.getName();
                    String contentType = file.getName().endsWith(".m3u8") 
                            ? "application/x-mpegURL" 
                            : "video/MP2T";

                    log.info("Uploading HLS file: filename={}, objectKey={}", file.getName(), outputObjectKey);
                    minioClient.uploadObject(
                            UploadObjectArgs.builder()
                                    .bucket(event.getBucketName())
                                    .object(outputObjectKey)
                                    .filename(file.getAbsolutePath())
                                    .contentType(contentType)
                                    .build()
                    );
                }
                log.info("Successfully processed and uploaded HLS video to folder: processed/{}/", lessonId);
                transcodeSuccess = true;
            } else {
                log.warn("No HLS files found in local directory: {}", localHlsFolder.getAbsolutePath());
            }

            if (transcodeSuccess) {
                // Delete temporary raw video from MinIO after successful transcode and upload
                log.info("Deleting temporary video from MinIO: bucket={}, objectKey={}", event.getBucketName(), event.getObjectKey());
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(event.getBucketName())
                                .object(event.getObjectKey())
                                .build()
                );
                publishProcessedEvent(lessonId, event.getBucketName(), "SUCCESS", durationMillis);
            } else {
                publishProcessedEvent(lessonId, event.getBucketName(), "FAILED", null);
            }

        } catch (Exception e) {
            log.error("Error occurred while processing HLS video upload event", e);
            publishProcessedEvent(lessonId, event.getBucketName(), "FAILED", null);
        } finally {
            // Step 6: Clean up temporary files and folder on host
            cleanupLocalFile(localInputFile);
            cleanupLocalDirectory(localHlsFolder);
        }
    }

    private void publishProcessedEvent(String lessonId, String bucketName, String status, Long durationMillis) {
        try {
            VideoProcessedEvent processedEvent = VideoProcessedEvent.builder()
                    .lessonId(lessonId)
                    .bucketName(bucketName)
                    .status(status)
                    .durationMillis(durationMillis)
                    .build();
            log.info("Publishing VideoProcessedEvent: lessonId={}, status={}, durationMillis={}",
                    lessonId, status, durationMillis);
            eventPublisher.publish(Exchanges.MEDIA, RoutingKeys.VIDEO_PROCESSED, processedEvent);
        } catch (Exception e) {
            log.error("Failed to publish VideoProcessedEvent for lessonId={}", lessonId, e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".mp4"; // default fallback
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void cleanupLocalFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("Deleted temporary file: {}", file.getAbsolutePath());
            } else {
                log.warn("Could not delete temporary file: {}", file.getAbsolutePath());
            }
        }
    }

    private void cleanupLocalDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    cleanupLocalFile(f);
                }
            }
            boolean deleted = dir.delete();
            if (deleted) {
                log.info("Deleted temporary directory: {}", dir.getAbsolutePath());
            } else {
                log.warn("Could not delete temporary directory: {}", dir.getAbsolutePath());
            }
        }
    }
}
