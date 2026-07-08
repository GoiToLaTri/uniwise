package com.uniwise.course_service.modules.course_mgmt.lesson.consumer;

import com.uniwise.course_service.configurations.RabbitMQConfig;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoUploadedConsumer {

    private final LessonRepository lessonRepository;

    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOADED_QUEUE)
    @Transactional
    public void handleVideoUploadedEvent(EventEnvelope<VideoUploadedEvent> envelope) {
        VideoUploadedEvent event = envelope.getPayload();
        String lessonId = event.getLessonId();

        log.info("Received VideoUploadedEvent for lessonId={}. Setting status back to PROCESSING.", lessonId);

        try {
            lessonRepository.findByPublicId(lessonId).ifPresentOrElse(lesson -> {
                lesson.setStatus(Lesson.LessonStatus.PROCESSING);
                lessonRepository.saveAndFlush(lesson);
                log.info("Successfully updated status to PROCESSING for lesson publicId={}", lessonId);
            }, () -> {
                log.warn("Lesson not found with publicId={} for video upload event", lessonId);
            });
        } catch (Exception e) {
            log.error("Failed to process VideoUploadedEvent for lesson publicId={}", lessonId, e);
        }
    }
}
