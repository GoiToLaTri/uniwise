package com.uniwise.user_service.modules.instructor.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.InstructorProfileCreateRequest;
import com.uniwise.common.dto.request.InstructorProfileUpdateRequest;
import com.uniwise.common.dto.response.DegreeDto;
import com.uniwise.common.dto.response.ExpertiseDto;
import com.uniwise.common.dto.response.InstructorProfileResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.InstructorError;
import com.uniwise.user_service.modules.instructor.InstructorService;
import com.uniwise.user_service.modules.instructor.entity.DegreeCertificate;
import com.uniwise.user_service.modules.instructor.entity.Expertise;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;
import com.uniwise.user_service.modules.instructor.mapper.InstructorMapper;
import com.uniwise.user_service.modules.instructor.repository.InstructorProfileRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InstructorServiceImpl implements InstructorService {
    InstructorProfileRepository instructorProfileRepository;
    InstructorMapper instructorMapper;

    @Override
    @PreAuthorize("hasAuthority('instructor:apply')")
    public InstructorProfileResponse applyInstructorProfile(InstructorProfileCreateRequest request) {
        String accountId = getCurrentAccountId();

        if (instructorProfileRepository.existsByAccountId(accountId)) {
            throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_ALREADY_EXISTS);
        }

        InstructorProfile instructorProfile = instructorMapper.toEntity(request);
        instructorProfile.setAccountId(accountId);
        instructorProfile.setPublicId(generatePublicId());
        instructorProfile.setStatus(EInstructorProfileStatus.PENDING);
        instructorProfile.setAppliedAt(LocalDateTime.now());

        assignChildEntities(instructorProfile);
        return instructorMapper.toResponse(instructorProfileRepository.save(instructorProfile));
    }

    @Override
    @PreAuthorize("hasAuthority('instructor:get-profile')")
    @Transactional(readOnly = true)
    public InstructorProfileResponse getMyInstructorProfile() {
        String accountId = getCurrentAccountId();
        return instructorProfileRepository.findByAccountId(accountId)
                .map(instructorMapper::toResponse)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));
    }

    @Override
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

        return instructorMapper.toResponse(instructorProfileRepository.save(instructorProfile));
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorProfileResponse getInstructorProfileByPublicId(String publicId) {
        InstructorProfile instructorProfile = instructorProfileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_FOUND));

        if (instructorProfile.getStatus() != EInstructorProfileStatus.APPROVED) {
            throw new HttpException(InstructorError.INSTRUCTOR_PROFILE_NOT_APPROVED);
        }

        return instructorMapper.toResponse(instructorProfile);
    }

    @Override
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

        return instructorMapper.toResponse(instructorProfileRepository.save(instructorProfile));
    }

    @Override
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

        return instructorMapper.toResponse(instructorProfileRepository.save(instructorProfile));
    }

    /**
     * Dùng khi hồ sơ giảng viên đang ở trạng thái approved, muốn đình chỉ
     */
    @Override
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

        return instructorMapper.toResponse(instructorProfileRepository.save(instructorProfile));
    }

    /**
     * Dùng khi hồ sơ giản viên đang ở trạng thái suspended
     */
    @Override
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

        return instructorMapper.toResponse(instructorProfileRepository.save(instructorProfile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorProfileResponse> listApplicationsByStatus(EInstructorProfileStatus status) {
        return instructorProfileRepository.findAllByStatus(status).stream()
                .map(instructorMapper::toResponse)
                .collect(Collectors.toList());
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
