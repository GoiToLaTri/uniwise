package com.uniwise.user_service.modules.instructor.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import com.uniwise.common.dto.request.InstructorProfileCreateRequest;
import com.uniwise.common.dto.request.InstructorProfileUpdateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.DegreeDto;
import com.uniwise.common.dto.response.ExpertiseDto;
import com.uniwise.common.dto.response.InstructorProfileResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PublicInstructorSearchResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.InstructorError;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;
import com.uniwise.identity.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.uniwise.identity.account.v1.AssignRolesRequest;
import com.uniwise.identity.account.v1.RevokeRolesRequest;
import com.uniwise.user_service.modules.instructor.InstructorService;
import com.uniwise.user_service.modules.instructor.entity.DegreeCertificate;
import com.uniwise.user_service.modules.instructor.entity.Expertise;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;
import com.uniwise.user_service.modules.instructor.event.InstructorSearchEventPublisher;
import com.uniwise.user_service.modules.instructor.mapper.InstructorMapper;
import com.uniwise.user_service.modules.instructor.repository.InstructorProfileRepository;
import com.uniwise.user_service.modules.profile.ProfileService;
import com.uniwise.user_service.modules.profile.entity.Profile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InstructorServiceImpl implements InstructorService {
    InstructorProfileRepository instructorProfileRepository;
    InstructorMapper instructorMapper;
    ProfileService profileService;
    InstructorSearchEventPublisher instructorSearchEventPublisher;

    @NonFinal
    @GrpcClient("identity-service")
    AccountServiceBlockingStub accountServiceClient;

    @Override
    @PreAuthorize("hasAuthority('instructor:apply')")
    @Transactional
    public InstructorProfileResponse applyInstructorProfile(InstructorProfileCreateRequest request) {
        String accountId = getCurrentAccountId();

        if (instructorProfileRepository.existsByAccountId(accountId)) {
            throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_EXISTS);
        }

        Profile profile = profileService.getProfileEntityForInternalUse(accountId);

        InstructorProfile instructorProfile = instructorMapper.toEntity(request);
        instructorProfile.setProfile(profile);
        instructorProfile.setPublicId(generatePublicId());
        instructorProfile.setStatus(EInstructorProfileStatus.PENDING);
        instructorProfile.setAppliedAt(LocalDateTime.now());

        assignChildEntities(instructorProfile);
        InstructorProfile saved = instructorProfileRepository.saveAndFlush(instructorProfile);
        instructorSearchEventPublisher.publish(saved);
        return instructorMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorProfileResponse getMyInstructorProfile() {
        String accountId = getCurrentAccountId();
        return instructorProfileRepository.findByAccountId(accountId)
                .map(instructorMapper::toResponse)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));
    }

    @Override
    @Transactional
    public InstructorProfileResponse updateMyInstructorProfile(InstructorProfileUpdateRequest request) {
        String accountId = getCurrentAccountId();
        InstructorProfile instructorProfile = instructorProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));

        instructorMapper.updateEntity(request, instructorProfile);

        if (request.getDegrees() != null) {
            replaceDegrees(instructorProfile, request.getDegrees());
        }

        if (request.getExpertises() != null) {
            replaceExpertises(instructorProfile, request.getExpertises());
        }

        instructorProfile.setStatus(EInstructorProfileStatus.PENDING);

        InstructorProfile saved = instructorProfileRepository.saveAndFlush(instructorProfile);
        instructorSearchEventPublisher.publish(saved);
        return instructorMapper.toResponse(saved);
    }

    @Override
    @PreAuthorize("hasAuthority('instructor:get-by-account-id')")
    @Transactional(readOnly = true)
    public InstructorProfileResponse getInstructorProfileByAccountId(String accountId) {
        InstructorProfile instructorProfile = instructorProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));
        return instructorMapper.toResponse(instructorProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicInstructorSearchResponse getPublicInstructorProfile(String publicId) {
        return instructorProfileRepository
                .findByProfilePublicIdAndStatus(publicId, EInstructorProfileStatus.APPROVED)
                .map(instructorMapper::toPublicSearchResponse)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));
    }

    @Override
    @PreAuthorize("hasAuthority('instructor:approve')")
    @Transactional
    public InstructorProfileResponse approveInstructorProfile(String publicId, String reviewComment) {
        InstructorProfile instructorProfile = instructorProfileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));

        if (instructorProfile.getStatus() != EInstructorProfileStatus.PENDING) {
            if (instructorProfile.getStatus() == EInstructorProfileStatus.APPROVED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_APPROVED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.REJECTED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_REJECTED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.SUSPENDED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_SUSPENDED);
        }

        instructorProfile.setStatus(EInstructorProfileStatus.APPROVED);
        instructorProfile.setReviewComment(reviewComment);
        instructorProfile.setApprovedAt(LocalDateTime.now());
        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .profileType("INSTRUCTOR")
                .build();
        profileService.updateProfileByAccountId(instructorProfile.getProfile().getAccountId(), profileUpdateRequest);

        AssignRolesRequest assignRolesRequest = AssignRolesRequest.newBuilder()
                .setAccountId(instructorProfile.getProfile().getAccountId())
                .addRoleNames("INSTRUCTOR")
                .build();

        // Gọi grpc set lại vai trò cho account
        accountServiceClient.assignRoles(assignRolesRequest);
        // Response có thể dùng nếu cần kiểm tra trạng thái hoặc log
        log.info("Assigned role INSTRUCTOR to account {} via identity-service grpc", instructorProfile.getProfile().getAccountId());

        InstructorProfile saved = instructorProfileRepository.saveAndFlush(instructorProfile);
        instructorSearchEventPublisher.publish(saved);
        return instructorMapper.toResponse(saved);
    }

    @Override
    @PreAuthorize("hasAuthority('instructor:reject')")
    @Transactional
    public InstructorProfileResponse rejectInstructorProfile(String publicId, String reviewComment) {
        InstructorProfile instructorProfile = instructorProfileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));

        if (instructorProfile.getStatus() != EInstructorProfileStatus.PENDING) {
            if (instructorProfile.getStatus() == EInstructorProfileStatus.APPROVED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_APPROVED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.REJECTED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_REJECTED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.SUSPENDED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_SUSPENDED);
        }

        instructorProfile.setStatus(EInstructorProfileStatus.REJECTED);
        instructorProfile.setReviewComment(reviewComment);
        instructorProfile.setRejectedAt(LocalDateTime.now());

        InstructorProfile saved = instructorProfileRepository.saveAndFlush(instructorProfile);
        instructorSearchEventPublisher.publish(saved);
        return instructorMapper.toResponse(saved);
    }

    /**
     * Dùng khi hồ sơ giảng viên đang ở trạng thái approved, muốn đình chỉ
     */
    @Override
    @PreAuthorize("hasAuthority('instructor:suspend')")
    @Transactional
    public InstructorProfileResponse suspendInstructorProfile(String publicId, String reviewComment) {
        InstructorProfile instructorProfile = instructorProfileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));

        if (instructorProfile.getStatus() != EInstructorProfileStatus.APPROVED) {
            if (instructorProfile.getStatus() == EInstructorProfileStatus.PENDING)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_APPROVED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.REJECTED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_REJECTED);
        }

        instructorProfile.setStatus(EInstructorProfileStatus.SUSPENDED);
        instructorProfile.setReviewComment(reviewComment);
        instructorProfile.setSuspendedAt(LocalDateTime.now());

        // Gọi grpc để gỡ role INSTRUCTOR của account
        RevokeRolesRequest revokeRolesRequest = RevokeRolesRequest.newBuilder()
                .setAccountId(instructorProfile.getProfile().getAccountId())
                .addRoleNames("INSTRUCTOR")
                .build();
        accountServiceClient.revokeRoles(revokeRolesRequest);

        InstructorProfile saved = instructorProfileRepository.saveAndFlush(instructorProfile);
        instructorSearchEventPublisher.publish(saved);
        return instructorMapper.toResponse(saved);
    }

    /**
     * Dùng khi hồ sơ giản viên đang ở trạng thái suspended
     */
    @Override
    @PreAuthorize("hasAuthority('instructor:reactivate')")
    @Transactional
    public InstructorProfileResponse reactivateInstructorProfile(String publicId, String reviewComment) {
        InstructorProfile instructorProfile = instructorProfileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));

        if (instructorProfile.getStatus() != EInstructorProfileStatus.SUSPENDED) {
            if (instructorProfile.getStatus() == EInstructorProfileStatus.PENDING)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_APPROVED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.REJECTED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_REJECTED);
            else if (instructorProfile.getStatus() == EInstructorProfileStatus.APPROVED)
                throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_APPROVED);
        }

        instructorProfile.setStatus(EInstructorProfileStatus.APPROVED);
        instructorProfile.setReviewComment(reviewComment);
        instructorProfile.setReactivatedAt(LocalDateTime.now());

        // Gọi grpc để set lại role INSTRUCTOR của account
        AssignRolesRequest assignRolesRequest = AssignRolesRequest.newBuilder()
                .setAccountId(instructorProfile.getProfile().getAccountId())
                .addRoleNames("INSTRUCTOR")
                .build();
        accountServiceClient.assignRoles(assignRolesRequest);
    
        InstructorProfile saved = instructorProfileRepository.saveAndFlush(instructorProfile);
        instructorSearchEventPublisher.publish(saved);
        return instructorMapper.toResponse(saved);
    }

    @Override
    @PreAuthorize("hasAuthority('instructor:get-all')")
    @Transactional(readOnly = true)
    public PageResponse<InstructorProfileResponse> listApplicationsByStatus(EInstructorProfileStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<InstructorProfile> instructorPage = instructorProfileRepository.findAllByStatus(status, pageable);
        List<InstructorProfileResponse> content = instructorPage.getContent().stream()
                .map(instructorMapper::toResponse)
                .collect(Collectors.toList());
        
        return PageResponse.<InstructorProfileResponse>builder()
                .content(content)
                .pageNumber(instructorPage.getNumber())
                .pageSize(instructorPage.getSize())
                .totalElements(instructorPage.getTotalElements())
                .totalPages(instructorPage.getTotalPages())
                .last(instructorPage.isLast())
                .build();
    }

    private void replaceDegrees(InstructorProfile profile, List<DegreeDto> degreeDtos) {
        Set<DegreeCertificate> degrees = new HashSet<>();
        for (DegreeDto dto : degreeDtos) {
            DegreeCertificate degree = instructorMapper.toDegreeEntity(dto);
            degree.setInstructorProfile(profile);
            degrees.add(degree);
        }
        profile.getDegrees().clear();
        profile.getDegrees().addAll(degrees);
    }

    private void replaceExpertises(InstructorProfile profile, List<ExpertiseDto> expertiseDtos) {
        Set<Expertise> expertises = new HashSet<>();
        for (ExpertiseDto dto : expertiseDtos) {
            Expertise expertise = instructorMapper.toExpertiseEntity(dto);
            expertise.setInstructorProfile(profile);
            expertises.add(expertise);
        }
        profile.getExpertises().clear();
        profile.getExpertises().addAll(expertises);
    }

    private void assignChildEntities(InstructorProfile instructorProfile) {
        if (instructorProfile.getDegrees() != null) {
            instructorProfile.getDegrees().forEach(degree -> degree.setInstructorProfile(instructorProfile));
        }
        if (instructorProfile.getExpertises() != null) {
            instructorProfile.getExpertises().forEach(expertise -> expertise.setInstructorProfile(instructorProfile));
        }
    }

    private String getCurrentAccountId() {
        SecurityContext context = SecurityContextHolder.getContext();
        return java.util.Optional.ofNullable(context.getAuthentication())
                .map(authentication -> authentication.getName())
                .orElseThrow(() -> new HttpException(InstructorError.UNAUTHENTICATED));
    }

    private String generatePublicId() {
        String publicId;
        do {
            publicId = java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        } while (instructorProfileRepository.existsByPublicId(publicId));
        return publicId;
    }
}
