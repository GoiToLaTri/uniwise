package com.uniwise.course_service.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwise.common.exception.security.CustomAccessDeniedHandler;
import com.uniwise.common.exception.security.CustomAuthenticationEntryPoint;
import com.uniwise.jwt_security_starter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Định nghĩa một record để lưu thông tin endpoint public
    record Endpoint(String path, HttpMethod method) {
    }

    // Định nghĩa các endpoint public ở đây
    private static final Endpoint[] PUBLIC_ENDPOINT = {
            new Endpoint("/v3/api-docs", HttpMethod.GET),
            new Endpoint("/api/v1/profiles/public/{publicId}", HttpMethod.GET),
            new Endpoint("/api/v1/courses/published", HttpMethod.GET),
            new Endpoint("/api/v1/price-tiers", HttpMethod.GET),
            new Endpoint("/api/v1/price-tiers/{id}", HttpMethod.GET),
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    // Đăng ký các endpoint public
                    for (Endpoint endpoint : PUBLIC_ENDPOINT)
                        auth.requestMatchers(endpoint.method(), endpoint.path()).permitAll();
                    auth.anyRequest().authenticated();
                }).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Quan trọng: stateless
                );

        return httpSecurity.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Sử dụng Argon2 để hash password
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        // Trả về lỗi 401 Unauthorized khi chưa xác thực hoặc token không hợp lệ
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(ObjectMapper objeccMapper) {
        // Trả về lỗi 403 Forbidden khi đã xác thực nhưng không có quyền truy cập
        return new CustomAccessDeniedHandler(objeccMapper);
    }
}
