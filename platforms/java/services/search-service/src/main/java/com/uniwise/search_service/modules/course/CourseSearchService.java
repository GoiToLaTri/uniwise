package com.uniwise.search_service.modules.course;

import java.util.List;

import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.platform_event_contract.event.course.CourseMetricsSyncEvent;
import com.uniwise.search_service.modules.course.entity.CourseDocument;

public interface CourseSearchService {
    PageResponse<CourseDocument> searchCourses(String keyword, int page, int size);

    PageResponse<CourseDocument> searchPublishedCourses(String keyword, int page, int size);

    PageResponse<CourseDocument> searchCreatorCourses(String keyword, String status, String creatorId, int page, int size);

    void bulkUpdateMetrics(List<CourseMetricsSyncEvent.CourseMetricPayload> payloads);
}
