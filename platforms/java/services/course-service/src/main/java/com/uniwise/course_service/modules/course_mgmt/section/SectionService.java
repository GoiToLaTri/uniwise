package com.uniwise.course_service.modules.course_mgmt.section;

import com.uniwise.common.dto.request.SectionCreateRequest;
import com.uniwise.common.dto.request.SectionUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.SectionResponse;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;

public interface SectionService {
    SectionResponse create(SectionCreateRequest request);
    SectionResponse getByPublicId(String publicId);
    PageResponse<SectionResponse> getAll(
            int page, int size,
            String courseId, String keyword,
            String sortBy, String sortDir);
    SectionResponse update(String publicId, SectionUpdateRequest request);
    void delete(String publicId);
    Section getEntityByPublicId(String publicId);
}
