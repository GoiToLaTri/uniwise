package com.uniwise.course_service.modules.course_mgmt.lesson.consumer;

import com.uniwise.course_service.configurations.RabbitMQConfig;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoTranscodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoTranscodedConsumer {

    private final LessonRepository lessonRepository;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_TRANSCODED_QUEUE)
    @Transactional
    public void handleVideoTranscodedEvent(EventEnvelope<VideoTranscodedEvent> envelope) {
        VideoTranscodedEvent event = envelope.getPayload();
        String lessonId = event.getLessonId();
        String videoUrl = event.getVideoUrl();

        log.info("Received VideoTranscodedEvent: lessonId={}, status={}, videoUrl={}", lessonId, event.getStatus(), videoUrl);

        try {
            lessonRepository.findByPublicId(lessonId).ifPresentOrElse(lesson -> {
                if ("FAILED".equalsIgnoreCase(event.getStatus())) {
                    lesson.setStatus(Lesson.LessonStatus.FAILED);
                    log.warn("Video transcoding failed for lesson publicId={}. Updated status to FAILED.", lessonId);
                } else {
                    lesson.setStatus(Lesson.LessonStatus.READY);
                    lesson.setContentReference(videoUrl);
                    log.info("Successfully updated status to READY and contentReference to {} for lesson publicId={}", videoUrl, lessonId);
                }
                lessonRepository.saveAndFlush(lesson);
            }, () -> {
                log.warn("Lesson not found with publicId={} for transcoding completion", lessonId);
            });
        } catch (Exception e) {
            log.error("Failed to process VideoTranscodedEvent for lesson publicId={}", lessonId, e);
        }
    }
}
