package com.uniwise.common.exception.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwise.common.dto.response.ErrorResponse;
import com.uniwise.common.exception.errors.ErrorDefinition;
import com.uniwise.common.exception.errors.SecurityError;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomAccessDeniedHandler implements AccessDeniedHandler{
    ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorDefinition accessDeneiError = SecurityError.FORBIDDEN;
        ErrorResponse errorDetails = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(accessDeneiError.getHttpStatus().value())
                .code(accessDeneiError.getCode())
                .detail(accessDeneiError.getMessage())
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
        response.flushBuffer();
        
    }
}
