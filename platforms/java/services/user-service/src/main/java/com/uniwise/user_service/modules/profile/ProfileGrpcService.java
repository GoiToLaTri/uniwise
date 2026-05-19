package com.uniwise.user_service.modules.profile;

import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;

public interface ProfileGrpcService {
    CreateProfileResponse create(CreateProfileRequest request);
}
