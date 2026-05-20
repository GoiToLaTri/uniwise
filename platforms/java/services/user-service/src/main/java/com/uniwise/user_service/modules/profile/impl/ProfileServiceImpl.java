package com.uniwise.user_service.modules.profile.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.ProfileError;
import com.uniwise.user_service.modules.profile.ProfileService;
import com.uniwise.user_service.modules.profile.mapper.ProfileMapper;
import com.uniwise.user_service.modules.profile.repository.ProfileRepository;
import com.uniwise.user_service.modules.profile.entity.Profile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileServiceImpl implements ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;

    @Override
    public ProfileResponse getProfile() {
        String accountId = getCurrentAccountId();
        return profileRepository.findByAccountId(accountId)
                .map(profileMapper::toResponse)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));
    }

    @Override
    public ProfileResponse getProfileByPublicId(String publicId) {
        return profileRepository.findByPublicId(publicId)
                .map(profileMapper::toResponse)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));
    }

    @Override
    public ProfileResponse createProfile(ProfileCreateRequest request) {
        String accountId = request.getAccountId() != null ? request.getAccountId() : getCurrentAccountId();

        if (profileRepository.existsByAccountId(accountId)) {
            throw new HttpException(ProfileError.PROFILE_ALREADY_EXISTS);
        }

        if (profileRepository.existsByEmail(request.getEmail())) {
            throw new HttpException(ProfileError.EMAIL_ALREADY_EXISTS);
        }

        if (request.getPublicId() != null && !request.getPublicId().isBlank()
                && profileRepository.existsByPublicId(request.getPublicId())) {
            throw new HttpException(ProfileError.PUBLIC_ID_ALREADY_EXISTS);
        }

        Profile profile = profileMapper.toEntity(request);
        profile.setAccountId(accountId);

        if (profile.getPublicId() == null || profile.getPublicId().isBlank()) {
            profile.setPublicId(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16));
        }

        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Override
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        String accountId = getCurrentAccountId();
        Profile profile = profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));

        if (request.getPublicId() != null && !request.getPublicId().equals(profile.getPublicId())
                && profileRepository.existsByPublicId(request.getPublicId())) {
            throw new HttpException(ProfileError.PUBLIC_ID_ALREADY_EXISTS);
        }

        profileMapper.updateEntity(request, profile);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    private String getCurrentAccountId() {
        SecurityContext context = SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication())
                .map(authentication -> authentication.getName())
                .orElseThrow(() -> new HttpException(ProfileError.UNAUTHENTICATED));
    }
}
