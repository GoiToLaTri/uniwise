package com.uniwise.ffmpeg_worker.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.ffmpeg_worker.configuration.RabbitMQConfig;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class VideoUploadedConsumer {

    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOADED_QUEUE)
    public void handleVideoUploadedEvent(EventEnvelope<VideoUploadedEvent> envelope) {
        log.info("Received VideoUploadedEvent envelope: eventId={}, producer={}, timestamp={}",
                envelope.getEventId(), envelope.getProducer(), envelope.getTimestamp());
        
        VideoUploadedEvent event = envelope.getPayload();
        log.info("Video event payload: objectKey={}, bucketName={}, originalFilename={}, contentType={}, size={}",
                event.getObjectKey(), event.getBucketName(), event.getOriginalFilename(), event.getContentType(), event.getSize());
        
        log.info("Start processing video from MinIO object key: {}", event.getObjectKey());
    }
}
