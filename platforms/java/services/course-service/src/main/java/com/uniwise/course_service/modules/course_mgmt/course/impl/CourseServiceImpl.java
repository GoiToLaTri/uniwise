package com.uniwise.course_service.modules.course_mgmt.course.impl;

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

import com.uniwise.common.dto.request.CourseCreateRequest;
import com.uniwise.common.dto.request.CourseUpdateRequest;
import com.uniwise.common.dto.response.CourseResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.CourseError;
import com.uniwise.course_service.modules.course_mgmt.course.CourseService;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.mapper.CourseMapper;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.pricing.entity.PriceTier;
import com.uniwise.course_service.modules.pricing.repository.PriceTierRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    CourseRepository courseRepository;
    PriceTierRepository priceTierRepository;
    CourseMapper courseMapper;

    // ===== CREATE =====
    @Override
    @PreAuthorize("hasAuthority('course:create')")
    @Transactional(rollbackFor = Exception.class)
    public CourseResponse create(CourseCreateRequest request) {
        log.info("Creating course with title: '{}'", request.getTitle());

        // 1. Validation (Check PriceTier exists if provided)
        PriceTier priceTier = null;
        if (request.getPriceTierId() != null && !request.getPriceTierId().isBlank()) {
            priceTier = priceTierRepository.findById(request.getPriceTierId())
                    .orElseThrow(() -> new HttpException(CourseError.PRICE_TIER_NOT_FOUND));
        }

        // 2. Mapping
        Course course = courseMapper.toEntity(request);
        course.setId(UUID.randomUUID().toString());

        // Generate unique 16-character publicId
        String publicId;
        do {
            publicId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        } while (courseRepository.existsByPublicId(publicId));
        course.setPublicId(publicId);

        course.setCreatorId(getCurrentAccountId());
        course.setPriceTier(priceTier);
        course.setIsActive(true); // Always active initially

        if (request.getStatus() == null) {
            course.setStatus(ECourseStatus.DRAFT);
        }

        // 3. Persist
        Course saved = courseRepository.save(course);
        log.info("Course created successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        return courseMapper.toResponse(saved);
    }

    // ===== GET BY PUBLIC ID =====
    @Override
    @Transactional(readOnly = true)
    public CourseResponse getByPublicId(String publicId) {
        log.info("Fetching course by publicId: {}", publicId);
        return courseMapper.toResponse(getEntityByPublicId(publicId));
    }

    // ===== GET ALL (paginated/filtered) =====
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAll(
            int page, int size,
            String creatorId, String status, String keyword,
            String sortBy, String sortDir) {

        log.info("Listing courses - page={}, size={}, creatorId={}, status={}, keyword='{}'", page, size, creatorId,
                status, keyword);

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(direction, orderBy));

        ECourseStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = ECourseStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                // Return empty page or throw exception
            }
        }

        Page<Course> pageResult = courseRepository.searchCourses(creatorId, statusEnum, normalizedKeyword, pageable);

        return PageResponse.<CourseResponse>builder()
                .content(pageResult.getContent().stream()
                        .map(courseMapper::toResponse)
                        .collect(Collectors.toList()))
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    // ===== UPDATE =====
    @Override
    @PreAuthorize("hasAuthority('course:update')")
    @Transactional(rollbackFor = Exception.class)
    public CourseResponse update(String publicId, CourseUpdateRequest request) {
        log.info("Updating course with publicId: {}", publicId);
        Course course = getEntityByPublicId(publicId);

        // Optional: Check if updater is the creator (or is admin)
        // String currentAccountId = getCurrentAccountId();
        // if (!course.getCreatorId().equals(currentAccountId)) { ... }

        // Update PriceTier if provided
        if (request.getPriceTierId() != null) {
            if (request.getPriceTierId().isBlank()) {
                course.setPriceTier(null); // Set to free
            } else {
                PriceTier priceTier = priceTierRepository.findById(request.getPriceTierId())
                        .orElseThrow(() -> new HttpException(CourseError.PRICE_TIER_NOT_FOUND));
                course.setPriceTier(priceTier);
            }
        }

        courseMapper.updateEntity(request, course);
        Course saved = courseRepository.save(course);
        log.info("Course updated successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        return courseMapper.toResponse(saved);
    }

    // ===== DELETE (SOFT DELETE) =====
    @Override
    @PreAuthorize("hasAuthority('course:delete')")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String publicId) {
        log.info("Soft deleting course with publicId: {}", publicId);
        Course course = getEntityByPublicId(publicId);
        course.setIsActive(false);
        courseRepository.save(course);
        log.info("Course soft deleted successfully with publicId: {}", publicId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getMyCourses(
            int page, int size,
            String status, String keyword,
            String sortBy, String sortDir) {
        String currentAccountId = getCurrentAccountId();
        log.info("Fetching my courses for accountId: {}", currentAccountId);
        return getAll(page, size, currentAccountId, status, keyword, sortBy, sortDir);
    }

    // ===== INTERNAL =====
    @Override
    public Course getEntityByPublicId(String publicId) {
        return courseRepository.findByPublicId(publicId)
                .filter(Course::getIsActive)
                .orElseThrow(() -> new HttpException(CourseError.COURSE_NOT_FOUND));
    }

    private String getCurrentAccountId() {
        SecurityContext context = SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication())
                .map(authentication -> authentication.getName())
                .orElse("");
    }
}
