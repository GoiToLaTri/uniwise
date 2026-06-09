package com.uniwise.course_service.modules.pricing;

import com.uniwise.common.dto.request.CreatePriceTierRequest;
import com.uniwise.common.dto.request.UpdatePriceTierRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PriceTierResponse;

public interface PriceTierService {

    /**
     * Creates a new price tier.
     *
     * @param request validated creation payload
     * @return persisted price tier as response DTO
     * @throws com.example.common.exception.HttpException {@code PT_002} when a tier with the same name already exists
     */
    PriceTierResponse create(CreatePriceTierRequest request);

    /**
     * Retrieves a single price tier by its identifier.
     *
     * @param id price tier UUID
     * @return price tier response DTO
     * @throws com.example.common.exception.HttpException {@code PT_001} when not found
     */
    PriceTierResponse getById(String id);

    /**
     * Returns a paginated, optionally filtered list of price tiers.
     *
     * @param page    zero-based page index (clamped to {@code >= 0})
     * @param size    page size (clamped to {@code >= 1})
     * @param keyword optional search term applied to tierName and currency
     * @param sortBy  field name to sort by (defaults to {@code "createdAt"})
     * @param sortDir sort direction: {@code "asc"} or {@code "desc"} (defaults to {@code "desc"})
     * @return paginated price tier response
     */
    PageResponse<PriceTierResponse> getAll(
            int page,
            int size,
            String keyword,
            String sortBy,
            String sortDir
    );

    /**
     * Updates an existing price tier (patch semantics – {@code null} fields are ignored).
     *
     * @param id      price tier UUID
     * @param request partial update payload
     * @return updated price tier response DTO
     * @throws com.example.common.exception.HttpException {@code PT_001} when not found
     * @throws com.example.common.exception.HttpException {@code PT_002} when name conflicts with another tier
     */
    PriceTierResponse update(String id, UpdatePriceTierRequest request);

    /**
     * Permanently deletes a price tier.
     *
     * @param id price tier UUID
     * @throws com.example.common.exception.HttpException {@code PT_001} when not found
     * @throws com.example.common.exception.HttpException {@code PT_004} when the tier is still assigned to courses
     * @throws com.example.common.exception.HttpException {@code PT_003} on any other deletion failure
     */
    void delete(String id);
}
