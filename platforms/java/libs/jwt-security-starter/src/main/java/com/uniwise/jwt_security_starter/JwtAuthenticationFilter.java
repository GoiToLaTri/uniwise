package com.uniwise.jwt_security_starter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    JwtService jwtService;
    JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {

            String token = extractToken(request);
            if (token != null && !token.isEmpty()) {
                Claims claims = jwtService.extractClaims(token);
                String accountId = claims.getSubject();
                String scopes = claims.get("scope", String.class);

                if (accountId != null && scopes != null) {
                    List<SimpleGrantedAuthority> authorities = parseScopes(scopes);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            accountId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else
                log.info("No JWT token found in request");

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Tùy chọn: Clear context nếu token lỗi
            SecurityContextHolder.clearContext();
        }

        // 3. Luôn luôn gọi doFilter để request được tiếp tục
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // Cách 1: Lấy từ header (mặc định)
        String token = request.getHeader(jwtProperties.getTokenHeader());
        // Cách 2: Cho phép lấy từ Authorization header (Bearer token)
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer "))
                token = authHeader.substring(7);
            else
                token = null; // Không tìm thấy token hợp lệ
        }

        return token;
    }

    private List<SimpleGrantedAuthority> parseScopes(String scopes) {
        if (scopes == null || scopes.isEmpty())
            return Collections.emptyList();

        // Support comma-separated scopes: "ROLE_USER ROLE_ADMIN"
        return Arrays.stream(scopes.split(" "))
                .map(String::trim)
                .filter(scope -> !scope.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
