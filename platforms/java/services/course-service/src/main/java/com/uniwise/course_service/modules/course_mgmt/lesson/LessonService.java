package com.uniwise.course_service.modules.course_mgmt.lesson;

import com.uniwise.common.dto.request.LessonCreateRequest;
import com.uniwise.common.dto.request.LessonUpdateRequest;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;

public interface LessonService {
    LessonResponse create(LessonCreateRequest request);
    LessonResponse getByPublicId(String publicId);
    PageResponse<LessonResponse> getAll(
            int page, int size,
            String sectionId, String keyword,
            String lessonType, String status,
            String sortBy, String sortDir);
    LessonResponse update(String publicId, LessonUpdateRequest request);
    void delete(String publicId);
    void reorder(String sectionId, com.uniwise.common.dto.request.ReorderRequest request);
    Lesson getEntityByPublicId(String publicId);
    Lesson getEntityById(String id);
    long countByCourseId(String courseId);
}
