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
        Long durationMillis = event.getDurationMillis();

        log.info("Received VideoTranscodedEvent: lessonId={}, status={}, videoUrl={}, durationMillis={}",
                lessonId, event.getStatus(), videoUrl, durationMillis);

        try {
            lessonRepository.findByPublicId(lessonId).ifPresentOrElse(lesson -> {
                if (!"SUCCESS".equalsIgnoreCase(event.getStatus())) {
                    lesson.setStatus(Lesson.LessonStatus.FAILED);
                    log.warn("Video transcoding did not succeed for lesson publicId={} (status={}). Updated status to FAILED.",
                            lessonId, event.getStatus());
                } else if (durationMillis == null || durationMillis <= 0) {
                    lesson.setStatus(Lesson.LessonStatus.FAILED);
                    log.error("Video transcoding reported success with invalid duration for lesson publicId={}: {}. "
                            + "Updated status to FAILED.", lessonId, durationMillis);
                } else {
                    lesson.setStatus(Lesson.LessonStatus.READY);
                    lesson.setContentReference(videoUrl);
                    lesson.setDurationMillis(durationMillis);
                    log.info("Successfully updated lesson publicId={} to READY with contentReference={} and durationMillis={}",
                            lessonId, videoUrl, durationMillis);
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
