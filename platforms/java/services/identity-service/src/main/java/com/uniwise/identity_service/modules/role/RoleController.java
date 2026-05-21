package com.uniwise.identity_service.modules.role;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.uniwise.common.dto.request.RoleCreateRequest;
import com.uniwise.common.dto.request.RoleUpdateRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.RoleResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
    RoleService roleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoleResponse> create(@RequestBody @Valid RoleCreateRequest request) {
        RoleResponse response = roleService.create(request);
        return ApiResponse.<RoleResponse>builder()
                .code("CREATED")
                .message("Role created successfully")
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        RoleResponse response = roleService.getById(id);
        return ApiResponse.<RoleResponse>builder()
                .code("OK")
                .message("Role retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<RoleResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<RoleResponse> response = roleService.getAll(page, size, keyword, isActive, sortBy, sortDir);
        return ApiResponse.<PageResponse<RoleResponse>>builder()
                .code("OK")
                .message("Roles retrieved successfully")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @RequestBody @Valid RoleUpdateRequest request) {
        RoleResponse response = roleService.update(id, request);
        return ApiResponse.<RoleResponse>builder()
                .code("OK")
                .message("Role updated successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.<Void>builder()
                .code("NO_CONTENT")
                .message("Role deleted successfully")
                .build();
    }

    @PostMapping("/{id}/assign-permissions")
    public ApiResponse<RoleResponse> assignPermissions(@PathVariable Long id,
            @RequestBody List<String> permissionNames) {
        RoleResponse response = roleService.assignPermissions(id, Set.copyOf(permissionNames));
        return ApiResponse.<RoleResponse>builder()
                .code("OK")
                .message("Permissions assigned to role successfully")
                .data(response)
                .build();
    }

    @PostMapping("/{id}/revoke-permissions")
    public ApiResponse<RoleResponse> revokePermissions(@PathVariable Long id,
            @RequestBody List<String> permissionNames) {
        RoleResponse response = roleService.revokePermissions(id, Set.copyOf(permissionNames));
        return ApiResponse.<RoleResponse>builder()
                .code("OK")
                .message("Permissions revoked from role successfully")
                .data(response)
                .build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<Void> toggleActive(@PathVariable Long id) {
        roleService.toggleActive(id);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Role active status toggled successfully")
                .build();
    }
}
