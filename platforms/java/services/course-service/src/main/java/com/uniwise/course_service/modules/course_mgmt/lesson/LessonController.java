package com.uniwise.course_service.modules.course_mgmt.lesson;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.uniwise.common.dto.request.LessonCreateRequest;
import com.uniwise.common.dto.request.LessonUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.common.dto.response.PageResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonController {

    LessonService lessonService;

    // ===== POST /api/v1/lessons =====
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LessonResponse> create(@Valid @RequestBody LessonCreateRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .code("CREATED")
                .message("Lesson created successfully")
                .data(lessonService.create(request))
                .build();
    }

    // ===== GET /api/v1/lessons/{id} =====
    @GetMapping("/{id}")
    public ApiResponse<LessonResponse> getById(@PathVariable("id") String publicId) {
        return ApiResponse.<LessonResponse>builder()
                .code("OK")
                .message("Lesson retrieved successfully")
                .data(lessonService.getByPublicId(publicId))
                .build();
    }

    // ===== GET /api/v1/lessons =====
    @GetMapping
    public ApiResponse<PageResponse<LessonResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sectionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String lessonType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "sortOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ApiResponse.<PageResponse<LessonResponse>>builder()
                .code("OK")
                .message("Lessons retrieved successfully")
                .data(lessonService.getAll(page, size, sectionId, keyword, lessonType, status, sortBy, sortDir))
                .build();
    }

    // ===== PUT /api/v1/lessons/{id} =====
    @PutMapping("/{id}")
    public ApiResponse<LessonResponse> update(
            @PathVariable("id") String publicId,
            @Valid @RequestBody LessonUpdateRequest request) {
        return ApiResponse.<LessonResponse>builder()
                .code("OK")
                .message("Lesson updated successfully")
                .data(lessonService.update(publicId, request))
                .build();
    }

    // ===== DELETE /api/v1/lessons/{id} =====
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable("id") String publicId) {
        lessonService.delete(publicId);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Lesson deleted successfully")
                .build();
    }
}
