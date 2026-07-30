package com.uniwise.course_service.modules.course_mgmt.course.impl;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.CourseError;
import com.uniwise.course_service.modules.course_mgmt.course.CourseService;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.mapper.CourseMapper;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseSyncQueueRepository;
import com.uniwise.course_service.modules.pricing.entity.PriceTier;
import com.uniwise.course_service.modules.pricing.repository.PriceTierRepository;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.course.CourseCreatedEvent;
import com.uniwise.platform_event_contract.event.course.CourseDeletedEvent;
import com.uniwise.platform_event_contract.event.course.CourseUpdatedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdRequest;
import com.uniwise.user.profile.v1.ProfileServiceGrpc.ProfileServiceBlockingStub;
import com.uniwise.user.profile.v1.PublicProfile;

import java.time.Instant;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    @NonFinal
    @GrpcClient("user-service")
    ProfileServiceBlockingStub profileServiceClient;

    CourseRepository courseRepository;
    CourseSyncQueueRepository courseSyncQueueRepository;
    // TODO: cần sửa vì quy phạm quy tắc layer
    PriceTierRepository priceTierRepository;
    CourseMapper courseMapper;
    LearningProgressService learningProgressService;
    LessonRepository lessonRepository;
    EventPublisher eventPublisher;

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

        String creatorId = getCurrentAccountId();
        PublicProfile instructor = getInstructorProfile(creatorId);

        course.setCreatorId(creatorId);
        course.setInstructorPublicId(instructor.getPublicId());
        course.setInstructorName(instructor.getName());
        course.setInstructorAvatarUrl(instructor.getAvatarUrl());
        course.setPriceTier(priceTier);
        course.setIsActive(true); // Always active initially

        if (request.getStatus() == null) {
            course.setStatus(ECourseStatus.DRAFT);
        }

        // 3. Persist
        Course saved = courseRepository.save(course);
        log.info("Course created successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        // 4. Publish Event
        eventPublisher.publish(RoutingKeys.COURSE_CREATED, CourseCreatedEvent.builder()
                .id(saved.getId())
                .publicId(saved.getPublicId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .creatorId(saved.getCreatorId())
                .status(saved.getStatus().name())
                .thumbnailUrl(saved.getThumbnailUrl())
                .priceTierId(saved.getPriceTier() != null ? saved.getPriceTier().getId() : null)
                .instructorPublicId(saved.getInstructorPublicId())
                .instructorName(saved.getInstructorName())
                .instructorAvatarUrl(saved.getInstructorAvatarUrl())
                .build());

        return courseMapper.toResponse(saved);
    }

    // ===== GET BY PUBLIC ID =====
    @Override
    @Transactional(readOnly = true)
    public CourseResponse getByPublicId(String publicId) {
        log.info("Fetching course by publicId: {}", publicId);
        Course course = getEntityByPublicId(publicId);
        
        CourseResponse response = courseMapper.toResponse(course);
        
        String currentAccountId = getCurrentAccountId();
        
        boolean isCreator = course.getCreatorId() != null && course.getCreatorId().equals(currentAccountId);
        boolean isAdmin = hasAdminAuthority();
        boolean isEnrolled = false;

        if (currentAccountId != null && !currentAccountId.isBlank()) {
            isEnrolled = isCreator || isAdmin || learningProgressService.isEnrolled(currentAccountId, course.getId());
        }
        
        response.setIsEnrolled(isEnrolled);

        int totalLessons = 0;
        if (course.getSections() != null) {
            for (var section : course.getSections()) {
                if (section.getLessons() != null) {
                    totalLessons += section.getLessons().size();
                }
            }
        }
        response.setTotalLessonsCount(totalLessons);

        if (isEnrolled) {
            List<UserLesson> userLessons = learningProgressService.getUserLessonsProgress(currentAccountId, course.getId());

            Map<String, UserLesson> progressMap = userLessons.stream()
                    .collect(Collectors.toMap(ul -> ul.getLesson().getId(), ul -> ul, (ul1, ul2) -> ul1));
            
            int completedCount = 0;

            if (response.getSections() != null) {
                for (var secResponse : response.getSections()) {
                    if (secResponse.getLessons() != null) {
                        for (var lesResponse : secResponse.getLessons()) {
                            Lesson lessonEntity = lessonRepository.findById(lesResponse.getId()).orElse(null);
                            if (lessonEntity != null) {
                                lesResponse.setIsPreview(lessonEntity.getIsPreview());
                            } else {
                                lesResponse.setIsPreview(false);
                            }
                            
                            UserLesson progress = progressMap.get(lesResponse.getId());
                            if (progress != null) {
                                lesResponse.setIsCompleted(progress.getIsCompleted());
                                lesResponse.setLastWatchedPosition(progress.getLastWatchedPosition());
                                if (Boolean.TRUE.equals(progress.getIsCompleted())) {
                                    completedCount++;
                                }
                            } else {
                                lesResponse.setIsCompleted(false);
                                lesResponse.setLastWatchedPosition(0);
                            }
                        }
                    }
                }
            }
            
            response.setCompletedLessonsCount(completedCount);
            response.setProgressPercentage(totalLessons > 0 ? (double) completedCount * 100 / totalLessons : 0.0);

        } else {
            if (response.getSections() != null) {
                for (var secResponse : response.getSections()) {
                    if (secResponse.getLessons() != null) {
                        for (var lesResponse : secResponse.getLessons()) {
                            Lesson lessonEntity = lessonRepository.findById(lesResponse.getId()).orElse(null);
                            boolean isPreview = lessonEntity != null && Boolean.TRUE.equals(lessonEntity.getIsPreview());
                            
                            lesResponse.setIsPreview(isPreview);
                            
                            if (!isPreview) {
                                lesResponse.setContentReference(null);
                            }
                            
                            lesResponse.setIsCompleted(null);
                            lesResponse.setLastWatchedPosition(null);
                        }
                    }
                }
            }
            
            response.setCompletedLessonsCount(0);
            response.setProgressPercentage(0.0);
        }
        
        return response;
    }

    private boolean hasAdminAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("admin:all"));
    }

    // ===== GET ALL (paginated/filtered) =====
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAll(
            int page, int size,
            String creatorId, String instructorPublicId, String status, String keyword,
            String sortBy, String sortDir) {

        log.info(
                "Listing courses - page={}, size={}, creatorId={}, instructorPublicId={}, status={}, keyword='{}'",
                page, size, creatorId, instructorPublicId, status, keyword);

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String normalizedInstructorPublicId =
                (instructorPublicId == null || instructorPublicId.isBlank())
                        ? null
                        : instructorPublicId.trim();
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

        Page<Course> pageResult = courseRepository.searchCourses(
                creatorId, normalizedInstructorPublicId, statusEnum, normalizedKeyword, pageable);

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
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('course:update')")
    @Transactional(rollbackFor = Exception.class)
    public CourseResponse update(String publicId, CourseUpdateRequest request) {
        log.info("Updating course with publicId: {}", publicId);
        Course course = getEntityByPublicId(publicId);
        requireCreatorOrAdmin(course);

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

        // Publish Event
        publishCourseUpdatedEvent(saved);

        return courseMapper.toResponse(saved);
    }

    // ===== DELETE (SOFT DELETE) =====
    @Override
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('course:delete')")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String publicId) {
        log.info("Soft deleting course with publicId: {}", publicId);
        Course course = getEntityByPublicId(publicId);
        requireCreatorOrAdmin(course);
        course.setIsActive(false);
        courseRepository.save(course);
        log.info("Course soft deleted successfully with publicId: {}", publicId);

        // Publish Event
        eventPublisher.publish(RoutingKeys.COURSE_DELETED, CourseDeletedEvent.builder()
                .id(course.getId())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getMyCourses(
            int page, int size,
            String status, String keyword,
            String sortBy, String sortDir) {
        String currentAccountId = getCurrentAccountId();
        log.info("Fetching my courses for accountId: {}", currentAccountId);
        return getAll(page, size, currentAccountId, null, status, keyword, sortBy, sortDir);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public int backfillInstructorSnapshotsAndReindex() {
        List<Course> courses = courseRepository.findAllByIsActiveTrue();
        List<Course> coursesToUpdate = new ArrayList<>();
        Map<String, PublicProfile> profilesByCreator = new HashMap<>();

        for (Course course : courses) {
            if (hasInstructorSnapshot(course)) {
                continue;
            }

            PublicProfile profile = profilesByCreator.computeIfAbsent(
                    course.getCreatorId(),
                    this::getInstructorProfile);
            course.setInstructorPublicId(profile.getPublicId());
            course.setInstructorName(profile.getName());
            course.setInstructorAvatarUrl(profile.getAvatarUrl());
            coursesToUpdate.add(course);
        }

        if (!coursesToUpdate.isEmpty()) {
            courseRepository.saveAll(coursesToUpdate);
        }
        courses.forEach(this::publishCourseUpdatedEvent);
        log.info("Backfilled {} instructor snapshots and published reindex events for {} courses",
                coursesToUpdate.size(), courses.size());
        return courses.size();
    }

    @Override
    public void syncInstructorSnapshot(String accountId, String publicId, String name, String avatarUrl) {
        List<Course> courses = courseRepository.findAllByCreatorIdAndIsActiveTrue(accountId);
        List<Course> coursesToUpdate = new ArrayList<>();
        for (Course course : courses) {
            boolean snapshotChanged = !Objects.equals(course.getInstructorPublicId(), publicId)
                    || !Objects.equals(course.getInstructorName(), name)
                    || !Objects.equals(course.getInstructorAvatarUrl(), avatarUrl);
            if (snapshotChanged) {
                course.setInstructorPublicId(publicId);
                course.setInstructorName(name);
                course.setInstructorAvatarUrl(avatarUrl);
                coursesToUpdate.add(course);
            }
        }

        if (!coursesToUpdate.isEmpty()) {
            courseRepository.saveAll(coursesToUpdate);
        }

        courses.forEach(this::publishCourseUpdatedEvent);
        log.info("Synchronized instructor snapshot for account {}: updated={}, reindexed={}",
                accountId, coursesToUpdate.size(), courses.size());
    }

    // ===== INTERNAL =====
    @Override
    public Course getEntityByPublicId(String publicId) {
        return courseRepository.findByPublicId(publicId)
                .filter(Course::getIsActive)
                .orElseThrow(() -> new HttpException(CourseError.COURSE_NOT_FOUND));
    }

    @Override
    public Course getEntityById(String id) {
        return courseRepository.findById(id)
                .filter(Course::getIsActive)
                .orElseThrow(() -> new HttpException(CourseError.COURSE_NOT_FOUND));
    }

    @Override
    @Transactional
    public void incrementStudentCountAndQueueSync(String courseId) {
        courseRepository.incrementStudentCount(courseId);
        courseSyncQueueRepository.insertIgnoreCourseId(courseId);
    }

    @Override
    @Transactional
    public void incrementTotalSectionsAndQueueSync(String courseId) {
        courseRepository.incrementTotalSections(courseId);
        courseSyncQueueRepository.insertIgnoreCourseId(courseId);
    }

    @Override
    @Transactional
    public void decrementTotalSectionsAndQueueSync(String courseId) {
        courseRepository.decrementTotalSections(courseId);
        courseSyncQueueRepository.insertIgnoreCourseId(courseId);
    }

    @Override
    @Transactional
    public void incrementTotalLessonsAndQueueSync(String courseId) {
        courseRepository.incrementTotalLessons(courseId);
        courseSyncQueueRepository.insertIgnoreCourseId(courseId);
    }

    @Override
    @Transactional
    public void decrementTotalLessonsAndQueueSync(String courseId) {
        courseRepository.decrementTotalLessons(courseId);
        courseSyncQueueRepository.insertIgnoreCourseId(courseId);
    }

    private String getCurrentAccountId() {
        SecurityContext context = SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication())
                .map(authentication -> authentication.getName())
                .orElse("");
    }

    private PublicProfile getInstructorProfile(String accountId) {
        try {
            PublicProfile profile = profileServiceClient
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getPublicProfileByAccountId(
                            GetPublicProfileByAccountIdRequest.newBuilder()
                                    .setAccountId(accountId)
                                    .build())
                    .getProfile();

            if (profile.getPublicId().isBlank() || profile.getName().isBlank()) {
                throw new HttpException(CourseError.INSTRUCTOR_PROFILE_NOT_FOUND);
            }

            return profile;
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new HttpException(CourseError.INSTRUCTOR_PROFILE_NOT_FOUND);
            }

            log.error("Unable to fetch instructor profile for course creation: status={}",
                    exception.getStatus().getCode());
            throw new HttpException(CourseError.INSTRUCTOR_PROFILE_SERVICE_UNAVAILABLE);
        }
    }

    private boolean hasInstructorSnapshot(Course course) {
        return course.getInstructorPublicId() != null
                && !course.getInstructorPublicId().isBlank()
                && course.getInstructorName() != null
                && !course.getInstructorName().isBlank();
    }

    private void publishCourseUpdatedEvent(Course course) {
        eventPublisher.publish(RoutingKeys.COURSE_UPDATED, CourseUpdatedEvent.builder()
                .id(course.getId())
                .publicId(course.getPublicId())
                .creatorId(course.getCreatorId())
                .title(course.getTitle())
                .description(course.getDescription())
                .status(course.getStatus().name())
                .thumbnailUrl(course.getThumbnailUrl())
                .priceTierId(course.getPriceTier() != null ? course.getPriceTier().getId() : null)
                .instructorPublicId(course.getInstructorPublicId())
                .instructorName(course.getInstructorName())
                .instructorAvatarUrl(course.getInstructorAvatarUrl())
                .updatedAt(Instant.now())
                .build());
    }

    private void requireCreatorOrAdmin(Course course) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        boolean isAdmin = isAuthenticated
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                                || "admin:all".equals(authority.getAuthority()));
        boolean isCreator = isAuthenticated
                && Objects.equals(course.getCreatorId(), authentication.getName());

        if (!isCreator && !isAdmin)
            throw new HttpException(AuthError.ACCESS_DENIED);
    }
}
