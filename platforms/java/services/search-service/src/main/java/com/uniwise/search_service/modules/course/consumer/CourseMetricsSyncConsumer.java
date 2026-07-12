package com.uniwise.search_service.modules.course.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.platform_event_contract.event.course.CourseMetricsSyncEvent;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.search_service.config.RabbitMQConfig;
import com.uniwise.search_service.modules.course.CourseSearchService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseMetricsSyncConsumer {

    CourseSearchService courseSearchService;

    @RabbitListener(queues = RabbitMQConfig.COURSE_METRICS_SYNC_QUEUE)
    public void handleCourseMetricsSync(EventEnvelope<CourseMetricsSyncEvent> envelope) {
        log.info("Received CourseMetricsSyncEvent with id: {}", envelope.getEventId());
        
        CourseMetricsSyncEvent event = envelope.getPayload();
        if (event != null && event.getMetrics() != null && !event.getMetrics().isEmpty()) {
            try {
                courseSearchService.bulkUpdateMetrics(event.getMetrics());
                log.info("Successfully processed bulk update for {} courses", event.getMetrics().size());
            } catch (Exception e) {
                log.error("Error processing bulk update for course metrics", e);
                throw e; // Để trigger retry hoặc gửi vào DLQ
            }
        }
    }
}
