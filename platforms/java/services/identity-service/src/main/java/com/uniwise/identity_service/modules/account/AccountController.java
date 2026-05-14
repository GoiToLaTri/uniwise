package com.uniwise.identity_service.modules.account;

import java.util.Set;

import org.springframework.http.HttpStatus;
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

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.AccountUpdateRequest;
import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.PageResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {
    AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> create(@RequestBody @Valid AccountCreateRequest request) {
        AccountResponse response = accountService.create(request);
        return ApiResponse.<AccountResponse>builder()
                .code("CREATED")
                .message("Account created successfully")
                .data(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getById(@PathVariable String id) {
        AccountResponse response = accountService.getById(id);
        return ApiResponse.<AccountResponse>builder()
                .code("OK")
                .message("Account retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<AccountResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<AccountResponse> response = accountService.getAll(page, size, keyword, isActive, sortBy, sortDir);
        return ApiResponse.<PageResponse<AccountResponse>>builder()
                .code("OK")
                .message("Accounts retrieved successfully")
                .data(response)
                .build();
    }

    @GetMapping("/profile")
    public ApiResponse<AccountResponse> getProfile() {
        AccountResponse response = accountService.getProfile();
        return ApiResponse.<AccountResponse>builder()
                .code("OK")
                .message("Profile retrieved successfully")
                .data(response)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountResponse> update(@PathVariable String id,
            @RequestBody @Valid AccountUpdateRequest request) {
        AccountResponse response = accountService.update(id, request);
        return ApiResponse.<AccountResponse>builder()
                .code("OK")
                .message("Account updated successfully")
                .data(response)
                .build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        return ApiResponse.<Void>builder()
                .code("NO_CONTENT")
                .message("Account deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<Void> toggleActive(@PathVariable String id) {
        accountService.toggleActive(id);
        return ApiResponse.<Void>builder()
                .code("OK")
                .message("Account active status toggled successfully")
                .build();
    }

    @PostMapping("/{id}/assign-roles")
    public ApiResponse<AccountResponse> assignRoles(@PathVariable String id,
            @RequestBody Set<String> roleNames) {
        AccountResponse response = accountService.assignRoles(id, roleNames);
        return ApiResponse.<AccountResponse>builder()
                .code("OK")
                .message("Roles assigned successfully")
                .data(response)
                .build();
    }

    @PostMapping("/{id}/revoke-roles")
    public ApiResponse<AccountResponse> revokeRoles(@PathVariable String id, @RequestBody Set<String> roleNames) {
        AccountResponse response = accountService.revokeRoles(id, roleNames);
        return ApiResponse.<AccountResponse>builder()
                .code("OK")
                .message("Roles revoked successfully")
                .data(response)
                .build();
    }
}
