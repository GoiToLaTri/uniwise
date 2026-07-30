package com.uniwise.course_service.modules.course_mgmt.course;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.uniwise.common.dto.request.CourseCreateRequest;
import com.uniwise.common.dto.request.CourseUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.CourseResponse;
import com.uniwise.common.dto.response.PageResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseController {

    CourseService courseService;

    // ===== POST /api/v1/courses =====
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CourseResponse> create(@Valid @RequestBody CourseCreateRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .code("CREATED")
                .message("Course created successfully")
                .data(courseService.create(request))
                .build();
    }

    // ===== GET /api/v1/courses/me =====
    @GetMapping("/me")
    public ApiResponse<PageResponse<CourseResponse>> getMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code("OK")
                .message("My courses retrieved successfully")
                .data(courseService.getMyCourses(page, size, status, keyword, sortBy, sortDir))
                .build();
    }

    // ===== GET /api/v1/courses/{id} =====
    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getById(@PathVariable("id") String publicId) {
        return ApiResponse.<CourseResponse>builder()
                .code("OK")
                .message("Course retrieved successfully")
                .data(courseService.getByPublicId(publicId))
                .build();
    }

    // ===== GET /api/v1/courses/published =====
    @GetMapping("/published")
    public ApiResponse<PageResponse<CourseResponse>> getPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String instructorPublicId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code("OK")
                .message("Published courses retrieved successfully")
                .data(courseService.getAll(
                        page, size, null, instructorPublicId, "PUBLISHED", null, sortBy, sortDir))
                .build();
    }

    // ===== GET /api/v1/courses =====
    @GetMapping
    public ApiResponse<PageResponse<CourseResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String creatorId,
            @RequestParam(required = false) String instructorPublicId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.<PageResponse<CourseResponse>>builder()
                .code("OK")
                .message("Courses retrieved successfully")
                .data(courseService.getAll(
                        page, size, creatorId, instructorPublicId, status, keyword, sortBy, sortDir))
                .build();
    }

    @PostMapping("/maintenance/instructor-snapshots/backfill")
    public ApiResponse<Integer> backfillInstructorSnapshotsAndReindex() {
        int processedCourses = courseService.backfillInstructorSnapshotsAndReindex();
        return ApiResponse.<Integer>builder()
                .code("OK")
                .message("Instructor snapshots backfilled and reindex events published")
                .data(processedCourses)
                .build();
    }

    // ===== PUT /api/v1/courses/{id} =====
    @PutMapping("/{id}")
    public ApiResponse<CourseResponse> update(
            @PathVariable("id") String publicId,
            @Valid @RequestBody CourseUpdateRequest request) {
        return ApiResponse.<CourseResponse>builder()
                .code("OK")
                .message("Course updated successfully")
                .data(courseService.update(publicId, request))
                .build();
    }

    // ===== DELETE /api/v1/courses/{id} =====
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable("id") String publicId) {
        courseService.delete(publicId);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Course soft deleted successfully")
                .build();
    }
}
