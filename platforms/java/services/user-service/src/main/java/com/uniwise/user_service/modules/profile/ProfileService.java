package com.uniwise.user_service.modules.profile;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse getProfile();
    ProfileResponse getProfileByPublicId(String publicId);
    ProfileResponse createProfile(ProfileCreateRequest request);
    ProfileResponse updateProfile(ProfileUpdateRequest request);
}
