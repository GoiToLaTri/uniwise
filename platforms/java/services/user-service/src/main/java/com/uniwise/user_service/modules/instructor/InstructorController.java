package com.uniwise.user_service.modules.instructor;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.uniwise.common.dto.request.InstructorProfileCreateRequest;
import com.uniwise.common.dto.request.InstructorProfileUpdateRequest;
import com.uniwise.common.dto.request.InstructorReviewRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.InstructorProfileResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/instructors")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InstructorController {
    InstructorService instructorService;

    @PostMapping("/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InstructorProfileResponse> applyInstructor(
            @RequestBody @Valid InstructorProfileCreateRequest request) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("CREATED")
                .data(instructorService.applyInstructorProfile(request))
                .message("Instructor application submitted")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<InstructorProfileResponse> getMyInstructorProfile() {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.getMyInstructorProfile())
                .message("Get my instructor profile success")
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<InstructorProfileResponse> updateMyInstructorProfile(
            @RequestBody @Valid InstructorProfileUpdateRequest request) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.updateMyInstructorProfile(request))
                .message("Update instructor profile success")
                .build();
    }

    @GetMapping("/{publicId}")
    public ApiResponse<InstructorProfileResponse> getInstructorProfileByPublicId(@PathVariable String publicId) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.getInstructorProfileByPublicId(publicId))
                .message("Get instructor profile by publicId success")
                .build();
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('instructor:get-all')")
    public ApiResponse<PageResponse<InstructorProfileResponse>> listApplications(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        EInstructorProfileStatus profileStatus = status == null ? EInstructorProfileStatus.PENDING
                : EInstructorProfileStatus.valueOf(status.toUpperCase());
        return ApiResponse.<PageResponse<InstructorProfileResponse>>builder()
                .code("OK")
                .data(instructorService.listApplicationsByStatus(profileStatus, page, size))
                .message("List instructor applications success")
                .build();
    }

    @PatchMapping("/applications/{publicId}/approve")
    public ApiResponse<InstructorProfileResponse> approveApplication(@PathVariable String publicId,
            @RequestBody InstructorReviewRequest request) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.approveInstructorProfile(publicId, request.getReviewComment()))
                .message("Approve instructor application success")
                .build();
    }

    @PatchMapping("/applications/{publicId}/reject")
    public ApiResponse<InstructorProfileResponse> rejectApplication(@PathVariable String publicId,
            @RequestBody InstructorReviewRequest request) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.rejectInstructorProfile(publicId, request.getReviewComment()))
                .message("Reject instructor application success")
                .build();
    }

    @PatchMapping("/applications/{publicId}/suspend")
    public ApiResponse<InstructorProfileResponse> suspendApplication(@PathVariable String publicId,
            @RequestBody InstructorReviewRequest request) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.suspendInstructorProfile(publicId, request.getReviewComment()))
                .message("Suspend instructor application success")
                .build();
    }

    @PatchMapping("/applications/{publicId}/reactivate")
    public ApiResponse<InstructorProfileResponse> reactivateApplication(@PathVariable String publicId,
            @RequestBody InstructorReviewRequest request) {
        return ApiResponse.<InstructorProfileResponse>builder()
                .code("OK")
                .data(instructorService.reactivateInstructorProfile(publicId, request.getReviewComment()))
                .message("Reactivate instructor application success")
                .build();
    }
}
