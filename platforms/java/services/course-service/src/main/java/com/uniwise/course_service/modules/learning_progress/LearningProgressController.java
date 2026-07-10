package com.uniwise.course_service.modules.learning_progress;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.uniwise.common.dto.request.SyncVideoPositionRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.CourseProgressResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.UserCourseDto;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/learning-progress")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LearningProgressController {

    LearningProgressService learningProgressService;

    // ===== GET /api/v1/learning-progress/my-courses =====
    @GetMapping("/my-courses")
    public ApiResponse<PageResponse<UserCourseDto>> getMyEnrolledCourses(
            Principal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<UserCourseDto>>builder()
                .code("OK")
                .message("My enrolled courses retrieved successfully")
                .data(learningProgressService.getMyEnrolledCourses(principal.getName(), page, size))
                .build();
    }

    // ===== POST /api/v1/learning-progress/courses/{courseId}/enroll-free =====
    @PostMapping("/courses/{courseId}/enroll-free")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> enrollFreeCourse(
            Principal principal,
            @PathVariable("courseId") String courseId) {
        learningProgressService.enrollFreeCourse(principal.getName(), courseId);
        return ApiResponse.<Void>builder()
                .code("CREATED")
                .message("Successfully enrolled in free course")
                .build();
    }

    // ===== GET /api/v1/learning-progress/courses/{courseId} =====
    @GetMapping("/courses/{courseId}")
    public ApiResponse<CourseProgressResponse> getCourseProgress(
            Principal principal,
            @PathVariable("courseId") String courseId) {
        return ApiResponse.<CourseProgressResponse>builder()
                .code("OK")
                .message("Course progress retrieved successfully")
                .data(learningProgressService.getCourseProgress(principal.getName(), courseId))
                .build();
    }

    // ===== PUT /api/v1/learning-progress/lessons/{lessonId}/sync-position =====
    @PutMapping("/lessons/{lessonId}/sync-position")
    public ApiResponse<Void> syncVideoPosition(
            Principal principal,
            @PathVariable("lessonId") String lessonId,
            @Valid @RequestBody SyncVideoPositionRequest request) {
        learningProgressService.syncVideoPosition(principal.getName(), lessonId, request);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Video position synced successfully")
                .build();
    }

    // ===== POST /api/v1/learning-progress/lessons/{lessonId}/complete =====
    @PostMapping("/lessons/{lessonId}/complete")
    public ApiResponse<Void> markLessonAsCompleted(
            Principal principal,
            @PathVariable("lessonId") String lessonId) {
        learningProgressService.markLessonAsCompleted(principal.getName(), lessonId);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Lesson marked as completed")
                .build();
    }
}
