package com.uniwise.course_service.modules.hashtag.impl;

import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.HashtagCreateRequest;
import com.uniwise.common.dto.request.HashtagUpdateRequest;
import com.uniwise.common.dto.response.HashtagResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.HashtagError;
import com.uniwise.course_service.modules.hashtag.HashtagService;
import com.uniwise.course_service.modules.hashtag.entity.Hashtag;
import com.uniwise.course_service.modules.hashtag.mapper.HashtagMapper;
import com.uniwise.course_service.modules.hashtag.repository.HashtagRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
 
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HashtagServiceImpl implements HashtagService {
 
    HashtagRepository hashtagRepository;
    HashtagMapper hashtagMapper;
 
    // ===== CREATE =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HashtagResponse create(HashtagCreateRequest request) {
        // Normalize trước khi check để tránh duplicate do case
        String normalizedName = request.getName().toLowerCase().trim();
 
        if (hashtagRepository.existsByName(normalizedName))
            throw new HttpException(HashtagError.HASHTAG_ALREADY_EXISTS);
 
        Hashtag hashtag = hashtagMapper.toEntity(request);
        // @PrePersist sẽ tự lowercase, nhưng set luôn để nhất quán
        hashtag.setName(normalizedName);
 
        Hashtag saved = hashtagRepository.save(hashtag);
        log.info("Hashtag created successfully with id: {}", saved.getId());
        return hashtagMapper.toResponse(saved);
    }
 
    // ===== GET BY ID =====
    @Override
    @Transactional
    public HashtagResponse getById(String id) {
        Hashtag hashtag = getEntityById(id);
        return hashtagMapper.toResponse(hashtag);
    }
 
    // ===== GET ALL (paginated) =====
    @Override
    @Transactional
    public PageResponse<HashtagResponse> getAll(
            int page, int size,
            String keyword, Boolean isVerified,
            String sortBy, String sortDir) {
 
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
 
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(direction, orderBy)
        );
 
        Page<Hashtag> hashtags = hashtagRepository.searchHashtags(
                normalizedKeyword, isVerified, pageable
        );
 
        return PageResponse.<HashtagResponse>builder()
                .content(hashtags.getContent().stream()
                        .map(hashtagMapper::toResponse)
                        .collect(Collectors.toList()))
                .pageNumber(hashtags.getNumber())
                .pageSize(hashtags.getSize())
                .totalElements(hashtags.getTotalElements())
                .totalPages(hashtags.getTotalPages())
                .last(hashtags.isLast())
                .build();
    }
 
    // ===== UPDATE =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HashtagResponse update(String id, HashtagUpdateRequest request) {
        Hashtag hashtag = getEntityById(id);
 
        // Nếu có thay đổi name, kiểm tra duplicate (loại trừ chính nó)
        if (request.getName() != null && !request.getName().isBlank()) {
            String normalizedName = request.getName().toLowerCase().trim();
            if (hashtagRepository.existsByNameAndIdNot(normalizedName, id))
                throw new HttpException(HashtagError.HASHTAG_ALREADY_EXISTS);
            request.setName(normalizedName);
        }
 
        hashtagMapper.updateEntity(request, hashtag);
        Hashtag saved = hashtagRepository.save(hashtag);
        log.info("Hashtag updated successfully with id: {}", saved.getId());
        return hashtagMapper.toResponse(saved);
    }
 
    // ===== DELETE =====
    @Override
    @PreAuthorize("hashAuthority('hashtag:delete')")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Hashtag hashtag = getEntityById(id);
        hashtagRepository.delete(hashtag);
        log.info("Hashtag deleted successfully with id: {}", id);
    }
 
    // ===== TOGGLE VERIFIED =====
    @Override
    @PreAuthorize("hashAuthority('hashtag:toggle-verified')")
    @Transactional(rollbackFor = Exception.class)
    public HashtagResponse toggleVerified(String id) {
        Hashtag hashtag = getEntityById(id);
        hashtag.setIsVerified(!hashtag.getIsVerified());
        Hashtag saved = hashtagRepository.save(hashtag);
        log.info("Hashtag [{}] isVerified toggled to: {}", id, saved.getIsVerified());
        return hashtagMapper.toResponse(saved);
    }
 
    // ===== INTERNAL: INCREMENT / DECREMENT COURSE COUNT =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementCourseCount(String id) {
        // Đảm bảo hashtag tồn tại trước khi update
        if (!hashtagRepository.existsById(id))
            throw new HttpException(HashtagError.HASHTAG_NOT_FOUND);
        hashtagRepository.incrementCourseCount(id);
        log.info("Hashtag [{}] courseCount incremented", id);
    }
 
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementCourseCount(String id) {
        if (!hashtagRepository.existsById(id))
            throw new HttpException(HashtagError.HASHTAG_NOT_FOUND);
        hashtagRepository.decrementCourseCount(id);
        log.info("Hashtag [{}] courseCount decremented", id);
    }
 
    // ===== INTERNAL: GET ENTITY =====
    @Override
    @Transactional
    public Hashtag getEntityById(String id) {
        return hashtagRepository.findById(id)
                .orElseThrow(() -> new HttpException(HashtagError.HASHTAG_NOT_FOUND));
    }
}
