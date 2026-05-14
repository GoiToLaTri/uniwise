package com.uniwise.identity_service.modules.permission;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.request.PermissionCreateRequest;
import com.uniwise.common.dto.request.PermissionUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PermissionResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {
    PermissionService permissionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PermissionResponse> create(@RequestBody @Valid PermissionCreateRequest request) {
        PermissionResponse response = permissionService.create(request);
        return ApiResponse.<PermissionResponse>builder()
                .code("CREATED")
                .message("Permission created successfully")
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionResponse> getById(@PathVariable Long id) {
        PermissionResponse response = permissionService.getById(id);
        return ApiResponse.<PermissionResponse>builder()
                .code("OK")
                .message("Permission retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<PermissionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<PermissionResponse> response = permissionService.getAll(page, size, keyword, sortBy, sortDir);
        return ApiResponse.<PageResponse<PermissionResponse>>builder()
                .code("OK")
                .message("Permissions retrieved successfully")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PermissionResponse> update(@PathVariable Long id, @RequestBody @Valid PermissionUpdateRequest request) {
        PermissionResponse response = permissionService.update(id, request);
        return ApiResponse.<PermissionResponse>builder()
                .code("OK")
                .message("Permission updated successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Permission deleted successfully")
                .build();
    }
}
