package com.uniwise.search_service.modules.instructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.response.AdminInstructorSearchResponse;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PublicInstructorSearchResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/search/instructors")
@RequiredArgsConstructor
public class InstructorSearchController {

    private final InstructorSearchService instructorSearchService;

    @GetMapping
    public ApiResponse<PageResponse<PublicInstructorSearchResponse>> searchPublicInstructors(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<PublicInstructorSearchResponse>>builder()
                .code("OK")
                .message("Search public instructors successfully")
                .data(instructorSearchService.searchPublicInstructors(keyword, page, size))
                .build();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('search:all-instructor')")
    public ApiResponse<PageResponse<AdminInstructorSearchResponse>> searchAdminInstructors(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<AdminInstructorSearchResponse>>builder()
                .code("OK")
                .message("Search instructor profiles successfully")
                .data(instructorSearchService.searchAdminInstructors(keyword, status, page, size))
                .build();
    }
}
