package com.uniwise.search_service.modules.course;

import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.search_service.modules.course.entity.CourseDocument;

public interface CourseSearchService {
    PageResponse<CourseDocument> searchCourses(String keyword, int page, int size);
    PageResponse<CourseDocument> searchPublishedCourses(String keyword, int page, int size);
}
