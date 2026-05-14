package com.uniwise.common.utils;

import java.util.Objects;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class ServletUtils {
    public static HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
    }

    public static String getUserAgent() {
        return getCurrentRequest().getHeader("User-Agent");
    }

    public static String getRemoteAddress() {
        HttpServletRequest request = getCurrentRequest();
        String remoteAddr = "";

        // Kiểm tra các header phổ biến khi đi qua Proxy/Load Balancer
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For có thể chứa danh sách IP, lấy cái đầu tiên
            remoteAddr = xForwardedFor.split(",")[0];
        }

        if (remoteAddr.isEmpty()) {
            remoteAddr = request.getRemoteAddr();
        }

        return remoteAddr;
    }
}
