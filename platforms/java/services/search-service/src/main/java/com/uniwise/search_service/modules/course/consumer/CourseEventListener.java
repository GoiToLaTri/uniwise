package com.uniwise.search_service.modules.course.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.course.CourseCreatedEvent;
import com.uniwise.platform_event_contract.event.course.CourseDeletedEvent;
import com.uniwise.platform_event_contract.event.course.CourseUpdatedEvent;
import com.uniwise.search_service.config.RabbitMQConfig;
import com.uniwise.search_service.modules.course.entity.CourseDocument;
import com.uniwise.search_service.modules.course.repository.CourseDocumentRepository;
import com.uniwise.search_service.modules.redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventListener {

    private final CourseDocumentRepository courseDocumentRepository;
    private final RedisService redisService;

    @RabbitListener(queues = RabbitMQConfig.COURSE_CREATED_QUEUE)
    public void handleCourseCreated(EventEnvelope<CourseCreatedEvent> envelope) {
        CourseCreatedEvent event = envelope.getPayload();
        log.info("Received CourseCreatedEvent for indexing: {}", event.getPublicId());

        CourseDocument doc = CourseDocument.builder()
                .id(event.getId())
                .publicId(event.getPublicId())
                .title(event.getTitle())
                .description(event.getDescription())
                .creatorId(event.getCreatorId())
                .instructorPublicId(event.getInstructorPublicId())
                .instructorName(event.getInstructorName())
                .instructorAvatarUrl(event.getInstructorAvatarUrl())
                .status(event.getStatus())
                .thumbnailUrl(event.getThumbnailUrl())
                .priceTierId(event.getPriceTierId())
                .build();

        courseDocumentRepository.save(doc);
        
        // Clear cache to ensure data consistency
        redisService.deleteKeysByPattern("search_courses::published:*");
    }

    @RabbitListener(queues = RabbitMQConfig.COURSE_UPDATED_QUEUE)
    public void handleCourseUpdated(EventEnvelope<CourseUpdatedEvent> envelope) {
        CourseUpdatedEvent event = envelope.getPayload();
        log.info("Received CourseUpdatedEvent for indexing: {}", event.getPublicId());

        CourseDocument doc = courseDocumentRepository.findById(event.getId())
                .orElseGet(() -> CourseDocument.builder().id(event.getId()).build());
        doc.setPublicId(event.getPublicId());
        if (event.getCreatorId() != null && !event.getCreatorId().isBlank()) {
            doc.setCreatorId(event.getCreatorId());
        }
        doc.setTitle(event.getTitle());
        doc.setDescription(event.getDescription());
        if (event.getInstructorPublicId() != null
                || event.getInstructorName() != null
                || event.getInstructorAvatarUrl() != null) {
            doc.setInstructorPublicId(event.getInstructorPublicId());
            doc.setInstructorName(event.getInstructorName());
            doc.setInstructorAvatarUrl(event.getInstructorAvatarUrl());
        }
        doc.setStatus(event.getStatus());
        doc.setThumbnailUrl(event.getThumbnailUrl());
        doc.setPriceTierId(event.getPriceTierId());
        courseDocumentRepository.save(doc);

        // Clear cache to ensure data consistency
        redisService.deleteKeysByPattern("search_courses::published:*");
    }

    @RabbitListener(queues = RabbitMQConfig.COURSE_DELETED_QUEUE)
    public void handleCourseDeleted(EventEnvelope<CourseDeletedEvent> envelope) {
        CourseDeletedEvent event = envelope.getPayload();
        log.info("Received CourseDeletedEvent for deletion from index: {}", event.getId());

        courseDocumentRepository.findById(event.getId()).ifPresent(doc -> {
            courseDocumentRepository.deleteById(event.getId());

            // Clear cache to ensure data consistency
            redisService.deleteKeysByPattern("search_courses::published:*");
        });
    }
}
