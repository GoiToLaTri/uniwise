package com.uniwise.platform_event_contract.event.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMetricsSyncEvent {
    
    private List<CourseMetricPayload> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseMetricPayload {
        private String courseId;
        private Integer studentCount;
        private Double averageRating;
        private Integer totalReviews;
        private Integer totalSections;
        private Integer totalLessons;
    }
}
