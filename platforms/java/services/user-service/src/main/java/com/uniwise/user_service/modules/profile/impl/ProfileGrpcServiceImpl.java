package com.uniwise.user_service.modules.profile.impl;

import org.springframework.stereotype.Service;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.common.dto.response.PublicProfileResponse;
import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdRequest;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdResponse;
import com.uniwise.user.profile.v1.Profile;
import com.uniwise.user.profile.v1.PublicProfile;
import com.uniwise.user_service.modules.profile.ProfileGrpcService;
import com.uniwise.user_service.modules.profile.ProfileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileGrpcServiceImpl implements ProfileGrpcService {
    ProfileService profileService;

    @Override
    public CreateProfileResponse create(CreateProfileRequest request) {
        log.info("Processing business logic for creating profile: {}", request.getName());

        ProfileResponse profile = profileService.createProfileForAccount(
                request.getAccountId(),
                ProfileCreateRequest.builder()
                        .email(request.getEmail())
                        .name(request.getName())
                        .build()
        );

        // Xây dựng Response từ dữ liệu đã lưu
        return CreateProfileResponse.newBuilder()
                .setProfile(Profile.newBuilder()
                        .setId(profile.getId())
                        .setAccountId(request.getAccountId())
                        .setEmail(request.getEmail())
                        .setName(request.getName())
                        .build())
                .build();
    }

    @Override
    public GetPublicProfileByAccountIdResponse getPublicProfileByAccountId(
            GetPublicProfileByAccountIdRequest request) {
        log.debug("Fetching public profile for an internal account");

        PublicProfileResponse profile = profileService.getPublicProfileForInternalUse(
                request.getAccountId());

        return GetPublicProfileByAccountIdResponse.newBuilder()
                .setProfile(PublicProfile.newBuilder()
                        .setPublicId(valueOrEmpty(profile.getPublicId()))
                        .setName(valueOrEmpty(profile.getName()))
                        .setAvatarUrl(valueOrEmpty(profile.getAvatarUrl()))
                        .build())
                .build();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
