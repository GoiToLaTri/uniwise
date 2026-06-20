package com.uniwise.course_service.modules.course_mgmt.section.impl;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.SectionCreateRequest;
import com.uniwise.common.dto.request.SectionUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.SectionResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.SectionError;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.course_mgmt.section.SectionService;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.course_mgmt.section.mapper.SectionMapper;
import com.uniwise.course_service.modules.course_mgmt.section.repository.SectionRepository;

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

    // ===== CREATE =====
    @Override
    @PreAuthorize("hasAuthority('section:create')")
    @Transactional(rollbackFor = Exception.class)
    public SectionResponse create(SectionCreateRequest request) {
        log.info("Creating section with title: '{}' in course: '{}'", request.getTitle(), request.getCourseId());

        // 1. Validation (Check Course exists)
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new HttpException(SectionError.COURSE_NOT_FOUND));

        // Check for sort order duplication in the same course
        if (sectionRepository.existsByCourseIdAndSortOrder(request.getCourseId(), request.getSortOrder())) {
            log.warn("Sort order {} already exists in course {}", request.getSortOrder(), request.getCourseId());
            throw new HttpException(SectionError.SECTION_SORT_ORDER_CONFLICT);
        }

        // 2. Mapping
        Section section = sectionMapper.toEntity(request);
        section.setId(UUID.randomUUID().toString());

        // Generate unique 16-character publicId
        String publicId;
        do {
            publicId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
        } while (sectionRepository.existsByPublicId(publicId));
        section.setPublicId(publicId);

        section.setCourse(course);

        // 3. Persist
        Section saved = sectionRepository.save(section);
        log.info("Section created successfully with id: {}, publicId: {}", saved.getId(), saved.getPublicId());

        return sectionMapper.toResponse(saved);
    }

    // ===== GET BY PUBLIC ID =====
    @Override
    @Transactional(readOnly = true)
    public SectionResponse getByPublicId(String publicId) {
        log.info("Fetching section by publicId: {}", publicId);
        return sectionMapper.toResponse(getEntityByPublicId(publicId));
    }

    // ===== GET ALL (paginated/filtered) =====
    @Override
    @Transactional(readOnly = true)
    public PageResponse<SectionResponse> getAll(
            int page, int size,
            String courseId, String keyword,
            String sortBy, String sortDir) {

        log.info("Listing sections - page={}, size={}, courseId={}, keyword='{}'", page, size, courseId, keyword);

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "sortOrder" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(direction, orderBy)
        );

        Page<Section> pageResult = sectionRepository.searchSections(courseId, normalizedKeyword, pageable);

        return PageResponse.<SectionResponse>builder()
                .content(pageResult.getContent().stream()
                        .map(sectionMapper::toResponse)
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
    @PreAuthorize("hasAuthority('section:update')")
    @Transactional(rollbackFor = Exception.class)
    public SectionResponse update(String publicId, SectionUpdateRequest request) {
        log.info("Updating section with publicId: {}", publicId);
        Section section = getEntityByPublicId(publicId);

        // Check if updating sort order and it conflicts with another section in the same course
        if (request.getSortOrder() != null &&
                sectionRepository.existsByCourseIdAndSortOrderAndPublicIdNot(
                        section.getCourse().getId(), request.getSortOrder(), publicId)) {
            log.warn("Sort order {} already exists in course {}", request.getSortOrder(), section.getCourse().getId());
            throw new HttpException(SectionError.SECTION_SORT_ORDER_CONFLICT);
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
        sectionRepository.delete(section);
        log.info("Section deleted successfully with publicId: {}", publicId);
    }

    // ===== INTERNAL =====
    @Override
    public Section getEntityByPublicId(String publicId) {
        return sectionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new HttpException(SectionError.SECTION_NOT_FOUND));
    }
}
