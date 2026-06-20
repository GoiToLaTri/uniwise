package com.uniwise.course_service.modules.course_mgmt.course;

import com.uniwise.common.dto.request.CourseCreateRequest;
import com.uniwise.common.dto.request.CourseUpdateRequest;
import com.uniwise.common.dto.response.CourseResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;

public interface CourseService {
    CourseResponse create(CourseCreateRequest request);
    CourseResponse getByPublicId(String publicId);
    PageResponse<CourseResponse> getAll(
            int page, int size,
            String creatorId, String status, String keyword,
            String sortBy, String sortDir);
    CourseResponse update(String publicId, CourseUpdateRequest request);
    void delete(String publicId);
    PageResponse<CourseResponse> getMyCourses(
            int page, int size,
            String status, String keyword,
            String sortBy, String sortDir);
    Course getEntityByPublicId(String publicId);
}
