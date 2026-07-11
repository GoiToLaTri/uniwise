package com.uniwise.search_service.modules.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.search_service.modules.course.entity.CourseDocument;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search/courses")
@RequiredArgsConstructor
public class CourseSearchController {

    private final CourseSearchService courseSearchService;

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('search:all-course')")
    public ApiResponse<PageResponse<CourseDocument>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        return ApiResponse.<PageResponse<CourseDocument>>builder()
                .code("OK")
                .message("Search all courses successfully")
                .data(courseSearchService.searchCourses(keyword, page, size))
                .build();
    }

    @GetMapping("/published")
    public ApiResponse<PageResponse<CourseDocument>> searchPublished(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        return ApiResponse.<PageResponse<CourseDocument>>builder()
                .code("OK")
                .message("Search published courses successfully")
                .data(courseSearchService.searchPublishedCourses(keyword, page, size))
                .build();
    }
}
