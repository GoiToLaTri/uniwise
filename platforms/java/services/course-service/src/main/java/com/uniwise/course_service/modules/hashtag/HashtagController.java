package com.uniwise.course_service.modules.hashtag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.request.HashtagCreateRequest;
import com.uniwise.common.dto.request.HashtagUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.HashtagResponse;
import com.uniwise.common.dto.response.PageResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HashtagController {
 
    HashtagService hashtagService;
 
    // ===== POST /api/v1/hashtags =====
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HashtagResponse> create(@Valid @RequestBody HashtagCreateRequest request) {
        return ApiResponse.<HashtagResponse>builder()
                .code("CREATED")
                .message("Hashtag created successfully")
                .data(hashtagService.create(request))
                .build();
    }
 
    // ===== GET /api/v1/hashtags/{id} =====
    @GetMapping("/{id}")
    public ApiResponse<HashtagResponse> getById(@PathVariable String id) {
        return ApiResponse.<HashtagResponse>builder()
                .code("OK")
                .message("Hashtag retrieved successfully")
                .data(hashtagService.getById(id))
                .build();
    }
 
    // ===== GET /api/v1/hashtags =====
    @GetMapping
    public ApiResponse<PageResponse<HashtagResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.<PageResponse<HashtagResponse>>builder()
                .code("OK")
                .message("Hashtags retrieved successfully")
                .data(hashtagService.getAll(page, size, keyword, isVerified, sortBy, sortDir))
                .build();
    }
 
    // ===== PUT /api/v1/hashtags/{id} =====
    @PutMapping("/{id}")
    public ApiResponse<HashtagResponse> update(
            @PathVariable String id,
            @Valid @RequestBody HashtagUpdateRequest request) {
        return ApiResponse.<HashtagResponse>builder()
                .code("OK")
                .message("Hashtag updated successfully")
                .data(hashtagService.update(id, request))
                .build();
    }
 
    // ===== DELETE /api/v1/hashtags/{id} (ADMIN) =====
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        hashtagService.delete(id);
    }
 
    // ===== PATCH /api/v1/hashtags/{id}/toggle (ADMIN) =====
    @PatchMapping("/{id}/toggle")
    public ApiResponse<HashtagResponse> toggleVerified(@PathVariable String id) {
        return ApiResponse.<HashtagResponse>builder()
                .code("OK")
                .message("Hashtag verification status toggled successfully")
                .data(hashtagService.toggleVerified(id))
                .build();
    }
}
