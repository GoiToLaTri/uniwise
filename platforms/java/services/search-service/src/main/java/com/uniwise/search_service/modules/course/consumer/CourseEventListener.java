package com.uniwise.search_service.modules.course.consumer;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.course.CourseCreatedEvent;
import com.uniwise.platform_event_contract.event.course.CourseDeletedEvent;
import com.uniwise.platform_event_contract.event.course.CourseUpdatedEvent;
import com.uniwise.search_service.modules.course.entity.CourseDocument;
import com.uniwise.search_service.modules.course.repository.CourseDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventListener {

    private final CourseDocumentRepository courseDocumentRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.course.created", durable = "true"),
            exchange = @Exchange(name = Exchanges.EVENTS, type = ExchangeTypes.TOPIC),
            key = RoutingKeys.COURSE_CREATED
    ))
    public void handleCourseCreated(EventEnvelope<CourseCreatedEvent> envelope) {
        CourseCreatedEvent event = envelope.getPayload();
        log.info("Received CourseCreatedEvent for indexing: {}", event.getPublicId());
        
        CourseDocument doc = CourseDocument.builder()
                .id(event.getId())
                .publicId(event.getPublicId())
                .title(event.getTitle())
                .description(event.getDescription())
                .creatorId(event.getCreatorId())
                .status(event.getStatus())
                .build();
                
        courseDocumentRepository.save(doc);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.course.updated", durable = "true"),
            exchange = @Exchange(name = Exchanges.EVENTS, type = ExchangeTypes.TOPIC),
            key = RoutingKeys.COURSE_UPDATED
    ))
    public void handleCourseUpdated(EventEnvelope<CourseUpdatedEvent> envelope) {
        CourseUpdatedEvent event = envelope.getPayload();
        log.info("Received CourseUpdatedEvent for indexing: {}", event.getPublicId());
        
        courseDocumentRepository.findById(event.getId()).ifPresent(doc -> {
            doc.setTitle(event.getTitle());
            doc.setDescription(event.getDescription());
            doc.setStatus(event.getStatus());
            courseDocumentRepository.save(doc);
        });
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.course.deleted", durable = "true"),
            exchange = @Exchange(name = Exchanges.EVENTS, type = ExchangeTypes.TOPIC),
            key = RoutingKeys.COURSE_DELETED
    ))
    public void handleCourseDeleted(EventEnvelope<CourseDeletedEvent> envelope) {
        CourseDeletedEvent event = envelope.getPayload();
        log.info("Received CourseDeletedEvent for deletion from index: {}", event.getId());
        
        courseDocumentRepository.deleteById(event.getId());
    }
}
