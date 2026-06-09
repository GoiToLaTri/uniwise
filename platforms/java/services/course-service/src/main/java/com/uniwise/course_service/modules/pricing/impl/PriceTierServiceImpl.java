package com.uniwise.course_service.modules.pricing.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.uniwise.common.dto.request.CreatePriceTierRequest;
import com.uniwise.common.dto.request.UpdatePriceTierRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PriceTierResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.PriceTierError;
import com.uniwise.course_service.modules.pricing.PriceTierService;
import com.uniwise.course_service.modules.pricing.entity.PriceTier;
import com.uniwise.course_service.modules.pricing.mapper.PriceTierMapper;
import com.uniwise.course_service.modules.pricing.repository.PriceTierRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PriceTierServiceImpl implements PriceTierService {

    PriceTierRepository priceTierRepository;
    PriceTierMapper priceTierMapper;

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    @PreAuthorize("hasAuthority('price-tier:create')")
    @Transactional
    public PriceTierResponse create(CreatePriceTierRequest request) {
        log.info("Creating price tier with name: {}", request.getTierName());

        if (priceTierRepository.existsByTierName(request.getTierName())) {
            log.warn("Price tier with name '{}' already exists", request.getTierName());
            throw new HttpException(PriceTierError.PRICE_TIER_ALREADY_EXISTS);
        }

        PriceTier priceTier = priceTierMapper.toEntity(request);
        priceTier.setId(UUID.randomUUID().toString());

        PriceTier saved = priceTierRepository.save(priceTier);
        log.info("Price tier created successfully with id: {}", saved.getId());

        return buildResponse(saved);
    }

    // ─── Read (single) ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PriceTierResponse getById(String id) {
        log.info("Fetching price tier by id: {}", id);

        PriceTier priceTier = findByIdOrThrow(id);
        return buildResponse(priceTier);
    }

    // ─── Read (paginated) ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PriceTierResponse> getAll(
            int page,
            int size,
            String keyword,
            String sortBy,
            String sortDir
    ) {
        // ── Normalize input ──────────────────────────────────────────────────
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);

        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "";
        String safeSortBy  = StringUtils.hasText(sortBy)  ? sortBy.trim()  : "createdAt";
        String safeSortDir = "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";

        log.info("Listing price tiers – page={}, size={}, keyword='{}', sortBy={}, sortDir={}",
                safePage, safeSize, safeKeyword, safeSortBy, safeSortDir);

        Sort sort = "asc".equals(safeSortDir)
                ? Sort.by(safeSortBy).ascending()
                : Sort.by(safeSortBy).descending();

        Pageable pageable = PageRequest.of(safePage, safeSize, sort);
        Page<PriceTier> pageResult = priceTierRepository.findAllByKeyword(safeKeyword, pageable);

        List<PriceTierResponse> content = pageResult.getContent()
                .stream()
                .map(this::buildResponse)
                .toList();

        return PageResponse.<PriceTierResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Override
    @PreAuthorize("hasAuthority('price-tier:update')")
    @Transactional
    public PriceTierResponse update(String id, UpdatePriceTierRequest request) {
        log.info("Updating price tier id: {}", id);

        PriceTier existing = findByIdOrThrow(id);

        // Duplicate name check – exclude the current record
        if (StringUtils.hasText(request.getTierName())
                && priceTierRepository.existsByTierNameAndIdNot(request.getTierName(), id)) {
            log.warn("Another price tier already uses the name '{}'", request.getTierName());
            throw new HttpException(PriceTierError.PRICE_TIER_ALREADY_EXISTS);
        }

        priceTierMapper.updateEntity(request, existing);
        PriceTier updated = priceTierRepository.save(existing);
        log.info("Price tier updated successfully: {}", updated.getId());

        return buildResponse(updated);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @Override
    @PreAuthorize("hasAuthority('price-tier:delete')")
    @Transactional
    public void delete(String id) {
        log.info("Deleting price tier id: {}", id);

        PriceTier existing = findByIdOrThrow(id);

        // Guard: do not delete if courses are still referencing this tier
        boolean hasCourses = existing.getCourses() != null && !existing.getCourses().isEmpty();
        if (hasCourses) {
            log.warn("Price tier '{}' cannot be deleted – {} course(s) still reference it",
                    id, existing.getCourses().size());
            throw new HttpException(PriceTierError.PRICE_TIER_IN_USE);
        }

        try {
            priceTierRepository.delete(existing);
            log.info("Price tier deleted: {}", id);
        } catch (Exception ex) {
            log.error("Unexpected error while deleting price tier id={}: {}", id, ex.getMessage(), ex);
            throw new HttpException(PriceTierError.PRICE_TIER_DELETE_FAILED);
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private PriceTier findByIdOrThrow(String id) {
        return priceTierRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Price tier not found: {}", id);
                    return new HttpException(PriceTierError.PRICE_TIER_NOT_FOUND);
                });
    }

    /**
     * Converts a {@link PriceTier} entity to its response DTO and enriches
     * it with the derived {@code courseCount} field.
     */
    private PriceTierResponse buildResponse(PriceTier priceTier) {
        PriceTierResponse response = priceTierMapper.toResponse(priceTier);
        int courseCount = (priceTier.getCourses() != null) ? priceTier.getCourses().size() : 0;
        response.setCourseCount(courseCount);
        return response;
    }
}
