package com.uniwise.identity_service.modules.authentication.data;

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
public class RedisToken {
    String sessionId;
    String accountId;
    Long expiresAt; // Timestamp in milliseconds
    String scope;
}
