package com.uniwise.course_service.modules.course_mgmt.section;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.uniwise.common.dto.request.SectionCreateRequest;
import com.uniwise.common.dto.request.SectionUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.SectionResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SectionController {

    SectionService sectionService;

    // ===== POST /api/v1/sections =====
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SectionResponse> create(@Valid @RequestBody SectionCreateRequest request) {
        return ApiResponse.<SectionResponse>builder()
                .code("CREATED")
                .message("Section created successfully")
                .data(sectionService.create(request))
                .build();
    }

    // ===== GET /api/v1/sections/{id} =====
    @GetMapping("/{id}")
    public ApiResponse<SectionResponse> getById(@PathVariable("id") String publicId) {
        return ApiResponse.<SectionResponse>builder()
                .code("OK")
                .message("Section retrieved successfully")
                .data(sectionService.getByPublicId(publicId))
                .build();
    }

    // ===== GET /api/v1/sections =====
    @GetMapping
    public ApiResponse<PageResponse<SectionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.<PageResponse<SectionResponse>>builder()
                .code("OK")
                .message("Sections retrieved successfully")
                .data(sectionService.getAll(page, size, courseId, keyword, sortBy, sortDir))
                .build();
    }

    // ===== PUT /api/v1/sections/{id} =====
    @PutMapping("/{id}")
    public ApiResponse<SectionResponse> update(
            @PathVariable("id") String publicId,
            @Valid @RequestBody SectionUpdateRequest request) {
        return ApiResponse.<SectionResponse>builder()
                .code("OK")
                .message("Section updated successfully")
                .data(sectionService.update(publicId, request))
                .build();
    }

    // ===== DELETE /api/v1/sections/{id} =====
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable("id") String publicId) {
        sectionService.delete(publicId);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Section deleted successfully")
                .build();
    }

    // ===== PUT /api/v1/sections/course/{courseId}/reorder =====
    @PutMapping("/course/{courseId}/reorder")
    public ApiResponse<Void> reorder(
            @PathVariable("courseId") String courseId,
            @Valid @RequestBody com.uniwise.common.dto.request.ReorderRequest request) {
        sectionService.reorder(courseId, request);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Sections reordered successfully")
                .build();
    }
}
