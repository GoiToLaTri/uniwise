package com.uniwise.user_service.modules.profile;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.user_service.modules.profile.enums.ProfileType;

public interface ProfileService {
    PageResponse<ProfileResponse> getAllProfiles(int page, int size, String keyword,
            ProfileType profileType,
            String sortBy, String sortDir);

    ProfileResponse getProfile();

    ProfileResponse getProfileByPublicId(String publicId);

    ProfileResponse createProfile(ProfileCreateRequest request);

    ProfileResponse updateProfile(ProfileUpdateRequest request);
}
