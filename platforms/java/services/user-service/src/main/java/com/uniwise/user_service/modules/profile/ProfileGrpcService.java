package com.uniwise.user_service.modules.profile;

import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdRequest;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdResponse;

public interface ProfileGrpcService {
    CreateProfileResponse create(CreateProfileRequest request);

    GetPublicProfileByAccountIdResponse getPublicProfileByAccountId(
            GetPublicProfileByAccountIdRequest request);
}
