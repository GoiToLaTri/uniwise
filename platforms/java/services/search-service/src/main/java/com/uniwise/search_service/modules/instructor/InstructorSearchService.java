package com.uniwise.search_service.modules.instructor;

import com.uniwise.common.dto.response.AdminInstructorSearchResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PublicInstructorSearchResponse;

public interface InstructorSearchService {

    PageResponse<PublicInstructorSearchResponse> searchPublicInstructors(
            String keyword,
            int page,
            int size);

    PageResponse<AdminInstructorSearchResponse> searchAdminInstructors(
            String keyword,
            String status,
            int page,
            int size);
}
