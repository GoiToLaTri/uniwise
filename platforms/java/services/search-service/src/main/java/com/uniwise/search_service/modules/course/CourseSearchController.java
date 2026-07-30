package com.uniwise.search_service.modules.course;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.search_service.modules.course.dto.CourseSearchResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search/courses")
@RequiredArgsConstructor
public class CourseSearchController {

        private final CourseSearchService courseSearchService;

        @GetMapping
        @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('search:all-course')")
        public ApiResponse<PageResponse<CourseSearchResponse>> search(
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                return ApiResponse.<PageResponse<CourseSearchResponse>>builder()
                                .code("OK")
                                .message("Search all courses successfully")
                                .data(courseSearchService.searchCourses(keyword, page, size))
                                .build();
        }

        @GetMapping("/published")
        public ApiResponse<PageResponse<CourseSearchResponse>> searchPublished(
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(required = false) String instructorPublicId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                return ApiResponse.<PageResponse<CourseSearchResponse>>builder()
                                .code("OK")
                                .message("Search published courses successfully")
                                .data(courseSearchService.searchPublishedCourses(
                                                keyword, instructorPublicId, page, size))
                                .build();
        }

        @GetMapping("/creator")
        public ApiResponse<PageResponse<CourseSearchResponse>> searchCreatorCourses(
                        Principal principal,
                        @RequestParam(defaultValue = "") String keyword,
                        @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                // Get the current creator's ID from the Principal object provided by Spring
                // Security
                String currentUserId = principal.getName();

                return ApiResponse.<PageResponse<CourseSearchResponse>>builder()
                                .code("OK")
                                .message("Search creator courses successfully")
                                .data(courseSearchService.searchCreatorCourses(keyword, status, currentUserId, page, size))
                                .build();
        }
}
