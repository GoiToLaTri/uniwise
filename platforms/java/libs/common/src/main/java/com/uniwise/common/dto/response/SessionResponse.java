package com.uniwise.common.dto.response;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionResponse {
    String id;
    String os;
    String browser;
    String deviceType;
    String ipAddress;
    Instant lastActivity;
    boolean isCurrentSession; // Để Client biết đây là thiết bị họ đang cầm
    boolean isRevoked;
}
