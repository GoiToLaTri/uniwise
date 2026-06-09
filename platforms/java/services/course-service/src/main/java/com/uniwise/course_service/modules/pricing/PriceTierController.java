package com.uniwise.course_service.modules.pricing;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.request.CreatePriceTierRequest;
import com.uniwise.common.dto.request.UpdatePriceTierRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PriceTierResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/price-tiers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class PriceTierController {

    PriceTierService priceTierService;

    // ─── POST /api/v1/price-tiers ─────────────────────────────────────────────

    /**
     * Creates a new price tier.
     * Returns HTTP 201 Created on success.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PriceTierResponse> create(
            @Valid @RequestBody CreatePriceTierRequest request
    ) {
        log.info("POST /api/v1/price-tiers – creating price tier '{}'", request.getTierName());
        PriceTierResponse response = priceTierService.create(request);
        return ApiResponse.<PriceTierResponse>builder()
                .code("CREATED")
                .data(response)
                .message("Price tier created successfully")
                .build();
    }

    // ─── GET /api/v1/price-tiers/{id} ────────────────────────────────────────

    /**
     * Retrieves a single price tier by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<PriceTierResponse> getById(@PathVariable String id) {
        log.info("GET /api/v1/price-tiers/{}", id);
        PriceTierResponse response = priceTierService.getById(id);
        return ApiResponse.<PriceTierResponse>builder()
                .code("OK")
                .data(response)
                .message("Get price tier by id success")
                .build();
    }

    // ─── GET /api/v1/price-tiers ─────────────────────────────────────────────

    /**
     * Returns a paginated, filterable list of price tiers.
     *
     * @param page     zero-based page index          (default 0)
     * @param size     number of records per page     (default 20)
     * @param keyword  optional search term           (default "")
     * @param isActive reserved for future active/inactive filtering
     * @param sortBy   field to sort by               (default "createdAt")
     * @param sortDir  sort direction: asc|desc       (default "desc")
     */
    @GetMapping
    public ApiResponse<PageResponse<PriceTierResponse>> getAll(
            @RequestParam(defaultValue = "0")   int     page,
            @RequestParam(defaultValue = "20")  int     size,
            @RequestParam(defaultValue = "")    String  keyword,
            @RequestParam(required = false)     Boolean isActive,   // reserved
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir
    ) {
        log.info("GET /api/v1/price-tiers – page={}, size={}, keyword='{}', sortBy={}, sortDir={}",
                page, size, keyword, sortBy, sortDir);
        PageResponse<PriceTierResponse> result =
                priceTierService.getAll(page, size, keyword, sortBy, sortDir);
        return ApiResponse.<PageResponse<PriceTierResponse>>builder()
                .code("OK")
                .data(result)
                .message("Get price tiers list success")
                .build();
    }

    // ─── PUT /api/v1/price-tiers/{id} ────────────────────────────────────────

    /**
     * Partially updates an existing price tier (patch semantics).
     */
    @PutMapping("/{id}")
    public ApiResponse<PriceTierResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdatePriceTierRequest request
    ) {
        log.info("PUT /api/v1/price-tiers/{}", id);
        PriceTierResponse response = priceTierService.update(id, request);
        return ApiResponse.<PriceTierResponse>builder()
                .code("OK")
                .data(response)
                .message("Price tier updated successfully")
                .build();
    }

    // ─── DELETE /api/v1/price-tiers/{id} ─────────────────────────────────────

    /**
     * Permanently deletes a price tier.
     * Fails with 409 Conflict if the tier is still assigned to any course.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/v1/price-tiers/{}", id);
        priceTierService.delete(id);
        return ApiResponse.<Void>builder()
                .code("OK")
                .data(null)
                .message("Price tier deleted successfully")
                .build();
    }
}