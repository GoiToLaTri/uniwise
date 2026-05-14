package com.uniwise.common.exception.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

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
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint{
    ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        ErrorDefinition unauthorizeError = SecurityError.UNAUTHORIZED;

        ErrorResponse errorDetails = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(unauthorizeError.getHttpStatus().value())
                .code(unauthorizeError.getCode())
                .detail(unauthorizeError.getMessage())
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
        response.flushBuffer();
    }
}
