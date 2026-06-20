package com.uniwise.course_service.modules.course_mgmt.lesson.impl;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.LessonCreateRequest;
import com.uniwise.common.dto.request.LessonUpdateRequest;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.LessonError;
import com.uniwise.course_service.modules.course_mgmt.lesson.LessonService;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.course_mgmt.lesson.mapper.LessonMapper;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.course_mgmt.section.repository.SectionRepository;

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

    // ===== CREATE =====
    @Override
    @PreAuthorize("hasAuthority('lesson:create')")
    @Transactional(rollbackFor = Exception.class)
    public LessonResponse create(LessonCreateRequest request) {
        log.info("Creating lesson with title: '{}' in section: '{}'", request.getTitle(), request.getSectionId());

        // 1. Validation (Check Section exists)
        Section section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new HttpException(LessonError.SECTION_NOT_FOUND));

        // Check for sort order duplication in the same section
        if (lessonRepository.existsBySectionIdAndSortOrder(request.getSectionId(), request.getSortOrder())) {
            log.warn("Sort order {} already exists in section {}", request.getSortOrder(), request.getSectionId());
            throw new HttpException(LessonError.LESSON_SORT_ORDER_CONFLICT);
        }

        // 2. Mapping
        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setId(UUID.randomUUID().toString());

        // Generate unique 16-character publicId
        String publicId;
        do {
            publicId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        } while (lessonRepository.existsByPublicId(publicId));
        lesson.setPublicId(publicId);

        lesson.setSection(section);
        lesson.setStatus(Lesson.LessonStatus.PROCESSING); // default status is PROCESSING

        // 3. Persist
        Lesson saved = lessonRepository.save(lesson);
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
        return lessonMapper.toResponse(getEntityByPublicId(publicId));
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

        return PageResponse.<LessonResponse>builder()
                .content(pageResult.getContent().stream()
                        .map(lessonMapper::toResponse)
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
    @PreAuthorize("hasAuthority('lesson:update')")
    @Transactional(rollbackFor = Exception.class)
    public LessonResponse update(String publicId, LessonUpdateRequest request) {
        log.info("Updating lesson with publicId: {}", publicId);
        Lesson lesson = getEntityByPublicId(publicId);

        // Check if updating sort order and it conflicts with another lesson in the same section
        if (request.getSortOrder() != null &&
                lessonRepository.existsBySectionIdAndSortOrderAndPublicIdNot(
                        lesson.getSection().getId(), request.getSortOrder(), publicId)) {
            log.warn("Sort order {} already exists in section {}", request.getSortOrder(), lesson.getSection().getId());
            throw new HttpException(LessonError.LESSON_SORT_ORDER_CONFLICT);
        }

        // Keep track of status change to trigger video processing updates later if status changes from ready/processing
        String oldStatus = lesson.getStatus().name();

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
        lessonRepository.delete(lesson);
        log.info("Lesson deleted successfully with publicId: {}", publicId);

        // TODO: Bắn message qua message broker để dọn dẹp các tài nguyên video liên quan trên MinIO (nếu cần).
    }

    // ===== INTERNAL =====
    @Override
    public Lesson getEntityByPublicId(String publicId) {
        return lessonRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(LessonError.LESSON_NOT_FOUND));
    }
}
