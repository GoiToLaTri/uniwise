package com.uniwise.course_service.modules.course_mgmt.lesson.impl;

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

import com.uniwise.common.dto.request.LessonCreateRequest;
import com.uniwise.common.dto.request.LessonUpdateRequest;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.LessonError;
import com.uniwise.common.exception.errors.ValidationError;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.helper.CourseServiceHelper;
import com.uniwise.course_service.modules.course_mgmt.lesson.LessonService;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.course_mgmt.lesson.mapper.LessonMapper;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
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
public class LessonServiceImpl implements LessonService {

    LessonRepository lessonRepository;
    SectionRepository sectionRepository;
    LessonMapper lessonMapper;
    CourseServiceHelper courseServiceHelper;
    LearningProgressService learningProgressService;

    // ===== CREATE =====
    @Override
    @PreAuthorize("hasAuthority('lesson:create')")
    @Transactional(rollbackFor = Exception.class)
    public LessonResponse create(LessonCreateRequest request) {
        log.info("Creating lesson with title: '{}' in section: '{}'", request.getTitle(), request.getSectionId());

        // 1. Validation (Check Section exists & ownership)
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new HttpException(LessonError.SECTION_NOT_FOUND));
        requireCreatorOrAdmin(section.getCourse());

        // 2. Mapping
        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setId(UUID.randomUUID().toString());

        // Handle sort order logic
        if (request.getSortOrder() != null) {
            lessonRepository.shiftSortOrderUp(request.getSectionId(), request.getSortOrder());
            lesson.setSortOrder(request.getSortOrder());
        } else {
            Integer maxSortOrder = lessonRepository.findMaxSortOrderBySectionId(request.getSectionId());
            lesson.setSortOrder(maxSortOrder + 1);
        }

        // Generate unique 16-character publicId if not provided by client
        String publicId = request.getPublicId();
        if (publicId == null || publicId.isBlank()) {
            do {
                publicId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
            } while (lessonRepository.existsByPublicId(publicId));
        } else {
            if (lessonRepository.existsByPublicId(publicId)) {
                throw new HttpException(ValidationError.INVALID_REQUEST_BODY);
            }
        }
        lesson.setPublicId(publicId);

        lesson.setSection(section);
        if (lesson.getLessonType() == Lesson.LessonType.QUIZ) {
            lesson.setStatus(Lesson.LessonStatus.READY);
        } else {
            lesson.setStatus(Lesson.LessonStatus.PROCESSING);
        }

        // 3. Persist
        Lesson saved = lessonRepository.save(lesson);
        courseServiceHelper.incrementTotalLessonsAndQueueSync(section.getCourse().getId());
        log.info("Lesson created successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        // 4. Inter-service call / Message Broker (Nếu có)
        if (saved.getLessonType() == Lesson.LessonType.VIDEO) {
            // TODO: Bắn message qua Message Broker (Kafka / RabbitMQ) sang service xử lý video để bắt đầu encode/process video.
            // Ví dụ: messageBroker.sendVideoProcessingMessage(saved.getId(), saved.getContentReference());
            log.info("[TODO] Video lesson created with id: {}. Publish message to broker for processing...", saved.getId());
        }

        // 5. Logging & Return
        return lessonMapper.toResponse(saved);
    }

    // ===== GET BY ID =====
    @Override
    @Transactional(readOnly = true)
    public LessonResponse getByPublicId(String publicId) {
        log.info("Fetching lesson by publicId: {}", publicId);
        Lesson lesson = getEntityByPublicId(publicId);
        Course course = lesson.getSection().getCourse();
        boolean hasFullAccess = hasFullCourseAccess(course);
        if (!isCourseVisibleToCaller(course, hasFullAccess)) {
            throw new HttpException(LessonError.LESSON_NOT_FOUND);
        }
        return toMaskedResponse(lesson, hasFullAccess);
    }

    // ===== GET ALL (paginated/filtered) =====
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LessonResponse> getAll(
            int page, int size,
            String sectionId, String keyword,
            String lessonType, String status,
            String sortBy, String sortDir) {

        log.info("Listing lessons - page={}, size={}, sectionId={}, keyword='{}'", page, size, sectionId, keyword);

        Map<String, Boolean> accessCache = new HashMap<>();
        if (sectionId != null && !sectionId.isBlank()) {
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> new HttpException(LessonError.SECTION_NOT_FOUND));
            Course course = section.getCourse();
            boolean hasFullAccess = hasFullCourseAccess(course);
            if (!isCourseVisibleToCaller(course, hasFullAccess)) {
                throw new HttpException(LessonError.SECTION_NOT_FOUND);
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

        Lesson.LessonType typeEnum = null;
        if (lessonType != null && !lessonType.isBlank()) {
            try {
                typeEnum = Lesson.LessonType.valueOf(lessonType.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new HttpException(LessonError.LESSON_TYPE_INVALID);
            }
        }

        Lesson.LessonStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Lesson.LessonStatus.valueOf(status.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                throw new HttpException(LessonError.LESSON_STATUS_INVALID);
            }
        }

        Page<Lesson> pageResult = lessonRepository.searchLessons(
                sectionId, normalizedKeyword, typeEnum, statusEnum, pageable
        );

        List<LessonResponse> content = pageResult.getContent().stream()
                .filter(lesson -> {
                    Course course = lesson.getSection().getCourse();
                    boolean hasFullAccess = accessCache.computeIfAbsent(course.getId(), id -> hasFullCourseAccess(course));
                    return isCourseVisibleToCaller(course, hasFullAccess);
                })
                .map(lesson -> {
                    boolean hasFullAccess = accessCache.getOrDefault(lesson.getSection().getCourse().getId(), false);
                    return toMaskedResponse(lesson, hasFullAccess);
                })
                .toList();

        return PageResponse.<LessonResponse>builder()
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
    @PreAuthorize("hasAuthority('lesson:update')")
    @Transactional(rollbackFor = Exception.class)
    public LessonResponse update(String publicId, LessonUpdateRequest request) {
        log.info("Updating lesson with publicId: {}", publicId);
        Lesson lesson = getEntityByPublicId(publicId);
        requireCreatorOrAdmin(lesson.getSection().getCourse());

        // Check if updating sort order
        if (request.getSortOrder() != null && !request.getSortOrder().equals(lesson.getSortOrder())) {
            Integer oldSortOrder = lesson.getSortOrder();
            Integer newSortOrder = request.getSortOrder();
            String sectionId = lesson.getSection().getId();

            if (oldSortOrder < newSortOrder) {
                lessonRepository.shiftSortOrderRangeDown(sectionId, oldSortOrder, newSortOrder);
            } else {
                lessonRepository.shiftSortOrderRangeUp(sectionId, newSortOrder, oldSortOrder);
            }
        }

        lessonMapper.updateEntity(request, lesson);
        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson updated successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        // TODO: Nếu có cập nhật video / status thay đổi, bắn message cập nhật hoặc thông báo cho service liên quan.
        
        return lessonMapper.toResponse(saved);
    }

    // ===== DELETE =====
    @Override
    @PreAuthorize("hasAuthority('lesson:delete')")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String publicId) {
        log.info("Deleting lesson with publicId: {}", publicId);
        Lesson lesson = getEntityByPublicId(publicId);
        requireCreatorOrAdmin(lesson.getSection().getCourse());
        String sectionId = lesson.getSection().getId();
        Integer deletedSortOrder = lesson.getSortOrder();

        lessonRepository.delete(lesson);
        lessonRepository.shiftSortOrderDown(sectionId, deletedSortOrder);
        courseServiceHelper.decrementTotalLessonsAndQueueSync(lesson.getSection().getCourse().getId());
        
        log.info("Lesson deleted successfully with publicId: {}", publicId);

        // TODO: Bắn message qua message broker để dọn dẹp các tài nguyên video liên quan trên MinIO (nếu cần).
    }

    // ===== REORDER =====
    @Override
    @PreAuthorize("hasAuthority('lesson:update')")
    @Transactional(rollbackFor = Exception.class)
    public void reorder(String sectionId, com.uniwise.common.dto.request.ReorderRequest request) {
        log.info("Bulk reordering lessons in section: {}", sectionId);
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new HttpException(LessonError.SECTION_NOT_FOUND));
        requireCreatorOrAdmin(section.getCourse());

        for (com.uniwise.common.dto.request.ReorderItemRequest item : request.getItems()) {
            Lesson lesson = lessonRepository.findByPublicId(item.getId())
                    .orElseThrow(() -> new HttpException(LessonError.LESSON_NOT_FOUND));
            if (lesson.getSection().getId().equals(sectionId)) {
                lesson.setSortOrder(item.getSortOrder());
                lessonRepository.save(lesson);
            }
        }
    }

    // ===== INTERNAL =====
    @Override
    public Lesson getEntityByPublicId(String publicId) {
        return lessonRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(LessonError.LESSON_NOT_FOUND));
    }

    @Override
    public Lesson getEntityById(String id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new HttpException(LessonError.LESSON_NOT_FOUND));
    }

    @Override
    public long countByCourseId(String courseId) {
        return lessonRepository.countByCourseId(courseId);
    }

    private LessonResponse toMaskedResponse(Lesson lesson, boolean hasFullAccess) {
        LessonResponse response = lessonMapper.toResponse(lesson);
        if (!hasFullAccess && !Boolean.TRUE.equals(lesson.getIsPreview())) {
            response.setContentReference(null);
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
