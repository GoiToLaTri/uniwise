package com.uniwise.user_service.modules.instructor;

import com.uniwise.common.dto.request.InstructorProfileCreateRequest;
import com.uniwise.common.dto.request.InstructorProfileUpdateRequest;
import com.uniwise.common.dto.response.InstructorProfileResponse;
import com.uniwise.common.dto.response.InstructorSearchReindexResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PublicInstructorProfileResponse;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;

public interface InstructorService {
    InstructorProfileResponse applyInstructorProfile(InstructorProfileCreateRequest request);

    InstructorProfileResponse getMyInstructorProfile();

    InstructorProfileResponse updateMyInstructorProfile(InstructorProfileUpdateRequest request);

    PublicInstructorProfileResponse getPublicInstructorProfile(String publicId);

    InstructorProfileResponse getInstructorProfileByAccountId(String accountId);

    InstructorProfileResponse approveInstructorProfile(String publicId, String reviewComment);

    InstructorProfileResponse rejectInstructorProfile(String publicId, String reviewComment);

    InstructorProfileResponse suspendInstructorProfile(String publicId, String reviewComment);

    InstructorProfileResponse reactivateInstructorProfile(String publicId, String reviewComment);

    PageResponse<InstructorProfileResponse> listApplicationsByStatus(EInstructorProfileStatus status, int page,
            int size);

    InstructorSearchReindexResponse reindexInstructorSearch(int page, int size);
}
