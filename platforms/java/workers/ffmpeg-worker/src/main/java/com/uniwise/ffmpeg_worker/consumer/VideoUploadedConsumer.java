package com.uniwise.ffmpeg_worker.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.ffmpeg_worker.configuration.RabbitMQConfig;
import com.uniwise.ffmpeg_worker.service.VideoTranscodeService;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoUploadedConsumer {

    private final VideoTranscodeService videoTranscodeService;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOADED_QUEUE)
    public void handleVideoUploadedEvent(EventEnvelope<VideoUploadedEvent> envelope) {
        log.info("Received VideoUploadedEvent envelope: eventId={}, producer={}, timestamp={}",
                envelope.getEventId(), envelope.getProducer(), envelope.getTimestamp());
        
        try {
            videoTranscodeService.transcodeVideoToHls(envelope);
            log.info("Successfully initiated and completed transcoding task for eventId={}", envelope.getEventId());
        } catch (Exception e) {
            log.error("Failed to process transcode event envelope for eventId={}", envelope.getEventId(), e);
        }
    }
}

