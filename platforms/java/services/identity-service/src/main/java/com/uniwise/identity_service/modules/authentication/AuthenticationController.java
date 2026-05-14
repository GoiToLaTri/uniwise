package com.uniwise.identity_service.modules.authentication;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.request.GetTokenRequest;
import com.uniwise.common.dto.request.RefreshTokenRequest;
import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.TokenResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/token")
    public ApiResponse<TokenResponse> getToken(@RequestBody @Valid GetTokenRequest request) {
        TokenResponse response = authenticationService.getToken(request);
        return ApiResponse.<TokenResponse>builder()
                .code("OK")
                .message("Token retrieved successfully")
                .data(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        TokenResponse response = authenticationService.refresh(request);
        return ApiResponse.<TokenResponse>builder()
                .code("OK")
                .message("Token refreshed successfully")
                .data(response)
                .build();
    }

    @GetMapping("/check-auth")
    public String check() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("User: {}", auth.getName());
        log.info("Authorities: {}", auth.getAuthorities());
        return "Authentication check completed";
    }

    // Các endpoint khác (refresh token, logout, get active sessions, revoke
    // session) sẽ được triển khai tương tự
}
