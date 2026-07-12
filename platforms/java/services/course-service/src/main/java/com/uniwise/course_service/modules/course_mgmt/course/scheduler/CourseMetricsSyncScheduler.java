package com.uniwise.course_service.modules.course_mgmt.course.scheduler;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.entity.CourseSyncQueue;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseSyncQueueRepository;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.course.CourseMetricsSyncEvent;
import com.uniwise.platform_event_contract.event.course.CourseMetricsSyncEvent.CourseMetricPayload;
import com.uniwise.platform_event_starter.publisher.EventPublisher;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseMetricsSyncScheduler {

    CourseSyncQueueRepository courseSyncQueueRepository;
    CourseRepository courseRepository;
    EventPublisher eventPublisher;

    // Chạy mỗi 5 phút
    @Scheduled(fixedRateString = "${course.metrics.sync.fixed-rate:300000}")
    public void syncCourseMetrics() {
        log.info("Starting scheduled task to sync course metrics...");
        Page<CourseSyncQueue> queuePage = courseSyncQueueRepository.findAll(
                PageRequest.of(0, 1000)
        );
        List<CourseSyncQueue> queueItems = queuePage.getContent();

        if (queueItems.isEmpty()) {
            log.info("No course metrics to sync.");
            return;
        }

        List<String> courseIds = queueItems.stream()
                .map(CourseSyncQueue::getCourseId)
                .toList();

        List<Course> coursesToSync = courseRepository.findAllById(courseIds);

        List<CourseMetricPayload> payloads = coursesToSync.stream()
                .map(course -> CourseMetricPayload.builder()
                        .courseId(course.getId())
                        .studentCount(course.getStudentCount())
                        .averageRating(course.getAverageRating())
                        .totalReviews(course.getTotalReviews())
                        .totalSections(course.getTotalSections())
                        .totalLessons(course.getTotalLessons())
                        .build())
                .collect(Collectors.toList());

        if (!payloads.isEmpty()) {
            CourseMetricsSyncEvent event = CourseMetricsSyncEvent.builder()
                    .metrics(payloads)
                    .build();

            eventPublisher.publish(Exchanges.EVENTS, RoutingKeys.COURSE_METRICS_SYNC, event);
            log.info("Published CourseMetricsSyncEvent for {} courses.", payloads.size());
        }

        courseSyncQueueRepository.deleteAll(queueItems);
        log.info("Cleared {} items from course sync queue.", queueItems.size());
    }
}
