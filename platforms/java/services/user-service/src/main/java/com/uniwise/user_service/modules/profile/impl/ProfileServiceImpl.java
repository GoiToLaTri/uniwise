package com.uniwise.user_service.modules.profile.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.common.dto.response.PublicProfileResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.ProfileError;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.profile.ProfileUpdatedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;
import com.uniwise.user_service.modules.profile.ProfileService;
import com.uniwise.user_service.modules.profile.entity.Profile;
import com.uniwise.user_service.modules.profile.enums.ProfileType;
import com.uniwise.user_service.modules.profile.mapper.ProfileMapper;
import com.uniwise.user_service.modules.profile.repository.ProfileRepository;

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
    EventPublisher eventPublisher;

    @Override
    public ProfileResponse getProfile() {
        String accountId = getCurrentAccountId();
        return profileRepository.findByAccountId(accountId)
                .map(profileMapper::toResponse)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));
    }

    // TODO: Method này sẽ được thay thế bằng elasticsearch hoặc search engine khác
    // trong tương lai để có hiệu năng tốt hơn
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<ProfileResponse> getAllProfiles(int page, int size, String keyword,
            ProfileType profileType,
            String sortBy, String sortDir) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, orderBy));

        Page<Profile> profiles = profileRepository.searchProfilesWithType(normalizedKeyword, profileType, pageable);
        List<ProfileResponse> content = profiles.getContent().stream()
                .map(profileMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ProfileResponse>builder()
                .content(content)
                .pageNumber(profiles.getNumber())
                .pageSize(profiles.getSize())
                .totalElements(profiles.getTotalElements())
                .totalPages(profiles.getTotalPages())
                .last(profiles.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfileResponse getProfileByPublicId(String publicId) {
        return profileRepository.findByPublicId(publicId)
                .map(profileMapper::toPublicResponse)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));
    }

    @Override
    @PreAuthorize("hasAuthority('profile:get-by-account-id')")
    @Transactional(readOnly = true)
    public PublicProfileResponse getProfileByAccountId(String accountId) {
        return profileRepository.findByAccountId(accountId)
                .map(profileMapper::toPublicResponse)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfileForInternalUse(String accountId) {
        return profileRepository.findByAccountId(accountId)
                .map(profileMapper::toPublicResponse)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional
    public ProfileResponse createProfile(ProfileCreateRequest request) {
        return createProfileForAccount(getCurrentAccountId(), request);
    }

    @Override
    @Transactional
    public ProfileResponse createProfileForAccount(String accountId, ProfileCreateRequest request) {
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
        // Mặc định tất cả profile được tạo ra đều có type là USER, sau này có thể
        // update lại nếu cần
        profile.setProfileType(ProfileType.USER);

        if (profile.getPublicId() == null || profile.getPublicId().isBlank()) {
            profile.setPublicId(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16));
        }

        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        return updateProfileForAccount(getCurrentAccountId(), request);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfileByAccountId(String accountId, ProfileUpdateRequest request) {
        return updateProfileForAccount(accountId, request);
    }

    private ProfileResponse updateProfileForAccount(String accountId, ProfileUpdateRequest request) {
        Profile profile = profileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new HttpException(ProfileError.PROFILE_NOT_FOUND));

        if (request.getPublicId() != null && !request.getPublicId().equals(profile.getPublicId())
                && profileRepository.existsByPublicId(request.getPublicId())) {
            throw new HttpException(ProfileError.PUBLIC_ID_ALREADY_EXISTS);
        }

        String previousPublicId = profile.getPublicId();
        String previousName = profile.getName();
        String previousAvatarUrl = profile.getAvatarUrl();

        profileMapper.updateEntity(request, profile);
        Profile saved = profileRepository.save(profile);

        if (!Objects.equals(previousPublicId, saved.getPublicId())
                || !Objects.equals(previousName, saved.getName())
                || !Objects.equals(previousAvatarUrl, saved.getAvatarUrl())) {
            eventPublisher.publish(RoutingKeys.PROFILE_UPDATED, ProfileUpdatedEvent.builder()
                    .accountId(saved.getAccountId())
                    .publicId(saved.getPublicId())
                    .name(saved.getName())
                    .avatarUrl(saved.getAvatarUrl())
                    .build());
        }

        return profileMapper.toResponse(saved);
    }

    private String getCurrentAccountId() {
        SecurityContext context = SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication())
                .map(authentication -> authentication.getName())
                .orElseThrow(() -> new HttpException(ProfileError.UNAUTHENTICATED));
    }
}
