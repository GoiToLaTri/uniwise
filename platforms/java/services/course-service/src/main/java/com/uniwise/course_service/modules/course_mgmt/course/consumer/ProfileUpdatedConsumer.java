package com.uniwise.course_service.modules.course_mgmt.course.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.course_service.configurations.RabbitMQConfig;
import com.uniwise.course_service.modules.course_mgmt.course.CourseService;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.profile.ProfileUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileUpdatedConsumer {

    private final CourseService courseService;

    @RabbitListener(queues = RabbitMQConfig.PROFILE_UPDATED_QUEUE)
    public void handleProfileUpdated(EventEnvelope<ProfileUpdatedEvent> envelope) {
        ProfileUpdatedEvent event = envelope.getPayload();
        if (event == null || event.getAccountId() == null || event.getAccountId().isBlank()) {
            log.warn("Ignoring invalid ProfileUpdatedEvent: eventId={}", envelope.getEventId());
            return;
        }

        courseService.syncInstructorSnapshot(
                event.getAccountId(),
                event.getPublicId(),
                event.getName(),
                event.getAvatarUrl());
    }
}
