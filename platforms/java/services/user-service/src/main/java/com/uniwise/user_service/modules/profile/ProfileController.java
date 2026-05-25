package com.uniwise.user_service.modules.profile;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.user_service.modules.profile.enums.ProfileType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileController {
    ProfileService profileService;

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> getMyProfile() {
        return ApiResponse.<ProfileResponse>builder()
                .code("OK")
                .data(profileService.getProfile())
                .message("Get profile success")
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<ProfileResponse>> getAllProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProfileType profileType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<ProfileResponse> response = profileService.getAllProfiles(page, size, keyword, profileType, sortBy,
                sortDir);
        return ApiResponse.<PageResponse<ProfileResponse>>builder()
                .code("OK")
                .message("Profiles retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping("/public/{publicId}")
    public ApiResponse<ProfileResponse> getProfileByPublicId(@PathVariable String publicId) {
        return ApiResponse.<ProfileResponse>builder()
                .code("OK")
                .data(profileService.getProfileByPublicId(publicId))
                .message("Get profile by publicId success")
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProfileResponse> createProfile(@RequestBody @Valid ProfileCreateRequest request) {
        return ApiResponse.<ProfileResponse>builder()
                .code("CREATED")
                .data(profileService.createProfile(request))
                .message("Create profile success")
                .build();
    }

    @PutMapping
    public ApiResponse<ProfileResponse> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        return ApiResponse.<ProfileResponse>builder()
                .code("OK")
                .data(profileService.updateProfile(request))
                .message("Update profile success")
                .build();
    }
}
