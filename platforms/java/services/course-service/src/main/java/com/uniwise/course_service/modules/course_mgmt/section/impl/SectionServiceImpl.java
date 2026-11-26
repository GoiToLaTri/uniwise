package com.uniwise.course_service.modules.course_mgmt.section.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.ReorderItemRequest;
import com.uniwise.common.dto.request.ReorderRequest;
import com.uniwise.common.dto.request.SectionCreateRequest;
import com.uniwise.common.dto.request.SectionUpdateRequest;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.SectionResponse;
import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.SectionError;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.course_mgmt.section.SectionService;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.course_mgmt.section.mapper.SectionMapper;
import com.uniwise.course_service.modules.course_mgmt.section.repository.SectionRepository;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SectionServiceImpl implements SectionService {

    SectionRepository sectionRepository;
    CourseRepository courseRepository;
    SectionMapper sectionMapper;
    com.uniwise.course_service.modules.course_mgmt.course.helper.CourseServiceHelper courseServiceHelper;
    LearningProgressService learningProgressService;

    // ===== CREATE =====
    @Override
    @PreAuthorize("hasAuthority('section:create')")
    @Transactional(rollbackFor = Exception.class)
    public SectionResponse create(SectionCreateRequest request) {
        log.info("Creating section with title: '{}' in course: '{}'", request.getTitle(), request.getCourseId());

        // 1. Validation (Check Course exists & ownership)
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new HttpException(SectionError.COURSE_NOT_FOUND));
        requireCreatorOrAdmin(course);

        // 2. Mapping
        Section section = sectionMapper.toEntity(request);
        section.setId(UUID.randomUUID().toString());

        // Handle sort order logic
        if (request.getSortOrder() != null) {
            sectionRepository.shiftSortOrderUp(request.getCourseId(), request.getSortOrder());
            section.setSortOrder(request.getSortOrder());
        } else {
            Integer maxSortOrder = sectionRepository.findMaxSortOrderByCourseId(request.getCourseId());
            section.setSortOrder(maxSortOrder + 1);
        }

        // Generate unique 16-character publicId
        String publicId;
        do {
            publicId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        } while (sectionRepository.existsByPublicId(publicId));
        section.setPublicId(publicId);

        section.setCourse(course);

        // 3. Persist
        Section saved = sectionRepository.save(section);
        courseServiceHelper.incrementTotalSectionsAndQueueSync(request.getCourseId());
        log.info("Section created successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        return sectionMapper.toResponse(saved);
    }

    // ===== GET BY PUBLIC ID =====
    @Override
    @Transactional(readOnly = true)
    public SectionResponse getByPublicId(String publicId) {
        log.info("Fetching section by publicId: {}", publicId);
        Section section = getEntityByPublicId(publicId);
        Course course = section.getCourse();
        boolean hasFullAccess = hasFullCourseAccess(course);
        if (!isCourseVisibleToCaller(course, hasFullAccess)) {
            throw new HttpException(SectionError.SECTION_NOT_FOUND);
        }
        return toMaskedResponse(section, hasFullAccess);
    }

    // ===== GET ALL (paginated/filtered) =====
    @Override
    @Transactional(readOnly = true)
    public PageResponse<SectionResponse> getAll(
            int page, int size,
            String courseId, String keyword,
            String sortBy, String sortDir) {

        log.info("Listing sections - page={}, size={}, courseId={}, keyword='{}'", page, size, courseId, keyword);

        Map<String, Boolean> accessCache = new HashMap<>();
        if (courseId != null && !courseId.isBlank()) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new HttpException(SectionError.COURSE_NOT_FOUND));
            boolean hasFullAccess = hasFullCourseAccess(course);
            if (!isCourseVisibleToCaller(course, hasFullAccess)) {
                throw new HttpException(SectionError.COURSE_NOT_FOUND);
            }
            accessCache.put(course.getId(), hasFullAccess);
        }

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "sortOrder" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(direction, orderBy)
        );

        Page<Section> pageResult = sectionRepository.searchSections(courseId, normalizedKeyword, pageable);

        List<SectionResponse> content = pageResult.getContent().stream()
                .filter(section -> {
                    Course course = section.getCourse();
                    boolean hasFullAccess = accessCache.computeIfAbsent(course.getId(), id -> hasFullCourseAccess(course));
                    return isCourseVisibleToCaller(course, hasFullAccess);
                })
                .map(section -> {
                    boolean hasFullAccess = accessCache.getOrDefault(section.getCourse().getId(), false);
                    return toMaskedResponse(section, hasFullAccess);
                })
                .toList();

        return PageResponse.<SectionResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    // ===== UPDATE =====
    @Override
    @PreAuthorize("hasAuthority('section:update')")
    @Transactional(rollbackFor = Exception.class)
    public SectionResponse update(String publicId, SectionUpdateRequest request) {
        log.info("Updating section with publicId: {}", publicId);
        Section section = getEntityByPublicId(publicId);
        requireCreatorOrAdmin(section.getCourse());

        // Check if updating sort order
        if (request.getSortOrder() != null && !request.getSortOrder().equals(section.getSortOrder())) {
            Integer oldSortOrder = section.getSortOrder();
            Integer newSortOrder = request.getSortOrder();
            String courseId = section.getCourse().getId();

            if (oldSortOrder < newSortOrder) {
                sectionRepository.shiftSortOrderRangeDown(courseId, oldSortOrder, newSortOrder);
            } else {
                sectionRepository.shiftSortOrderRangeUp(courseId, newSortOrder, oldSortOrder);
            }
        }

        sectionMapper.updateEntity(request, section);
        Section saved = sectionRepository.save(section);
        log.info("Section updated successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        return sectionMapper.toResponse(saved);
    }

    // ===== DELETE =====
    @Override
    @PreAuthorize("hasAuthority('section:delete')")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String publicId) {
        log.info("Deleting section with publicId: {}", publicId);
        Section section = getEntityByPublicId(publicId);
        requireCreatorOrAdmin(section.getCourse());
        String courseId = section.getCourse().getId();
        Integer deletedSortOrder = section.getSortOrder();
        int deletedLessonCount = section.getLessons() == null ? 0 : section.getLessons().size();

        sectionRepository.delete(section);
        sectionRepository.shiftSortOrderDown(courseId, deletedSortOrder);
        courseServiceHelper.decrementTotalSectionsAndQueueSync(courseId);
        if (deletedLessonCount > 0) {
            courseServiceHelper.decrementTotalLessonsByAmountAndQueueSync(courseId, deletedLessonCount);
        }

        log.info("Section deleted successfully with publicId: {}", publicId);
    }

    // ===== REORDER =====
    @Override
    @PreAuthorize("hasAuthority('section:update')")
    @Transactional(rollbackFor = Exception.class)
    public void reorder(String courseId, ReorderRequest request) {
        log.info("Bulk reordering sections in course: {}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new HttpException(SectionError.COURSE_NOT_FOUND));
        requireCreatorOrAdmin(course);

        for (ReorderItemRequest item : request.getItems()) {
            Section section = sectionRepository.findByPublicId(item.getId())
                    .orElseThrow(() -> new HttpException(SectionError.SECTION_NOT_FOUND));
            if (section.getCourse().getId().equals(courseId)) {
                section.setSortOrder(item.getSortOrder());
                sectionRepository.save(section);
            }
        }
    }

    // ===== INTERNAL =====
    @Override
    public Section getEntityByPublicId(String publicId) {
        return sectionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(SectionError.SECTION_NOT_FOUND));
    }

    private SectionResponse toMaskedResponse(Section section, boolean hasFullAccess) {
        SectionResponse response = sectionMapper.toResponse(section);
        if (!hasFullAccess && response.getLessons() != null) {
            for (LessonResponse lessonResponse : response.getLessons()) {
                if (!Boolean.TRUE.equals(lessonResponse.getIsPreview())) {
                    lessonResponse.setContentReference(null);
                }
            }
        }
        return response;
    }

    private boolean isCourseVisibleToCaller(Course course, boolean hasFullAccess) {
        if (course == null || !Boolean.TRUE.equals(course.getIsActive())) {
            return false;
        }
        return course.getStatus() == ECourseStatus.PUBLISHED || hasFullAccess;
    }

    private boolean hasFullCourseAccess(Course course) {
        if (course == null) {
            return false;
        }
        if (hasAdminAuthority()) {
            return true;
        }
        String currentAccountId = getCurrentAccountId();
        if (currentAccountId.isBlank()) {
            return false;
        }
        if (Objects.equals(course.getCreatorId(), currentAccountId)) {
            return true;
        }
        return learningProgressService.isEnrolled(currentAccountId, course.getId());
    }

    private void requireCreatorOrAdmin(Course course) {
        boolean isAdmin = hasAdminAuthority();
        String currentAccountId = getCurrentAccountId();
        boolean isCreator = !currentAccountId.isBlank()
                && course != null
                && Objects.equals(course.getCreatorId(), currentAccountId);

        if (!isCreator && !isAdmin) {
            throw new HttpException(AuthError.ACCESS_DENIED);
        }
    }

    private String getCurrentAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getName())) {
            return "";
        }
        return authentication.getName();
    }

    private boolean hasAdminAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "admin:all".equals(authority.getAuthority()));
    }
}
