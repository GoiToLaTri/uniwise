package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum AuthError implements ErrorDefinition {
        INVALID_CREDENTIALS(
                        "AUTH_001",
                        "Invalid credentials",
                        HttpStatus.UNAUTHORIZED),

        TOKEN_INVALID(
                        "AUTH_002",
                        "Token is invalid or corrupted",
                        HttpStatus.UNAUTHORIZED),

        TOKEN_EXPIRED(
                        "AUTH_003",
                        "Token has expired",
                        HttpStatus.UNAUTHORIZED),

        TOKEN_COMPROMISED(
                        "AUTH_004",
                        "Security alert: This token has been used before. Potential theft detected!",
                        HttpStatus.FORBIDDEN),

        SESSION_NOT_FOUND(
                        "AUTH_005",
                        "Session not found or has been revoked",
                        HttpStatus.NOT_FOUND),
        ACCESS_DENIED(
                        "AUTH_006",
                        "Access denied: You do not have permission to access this resource",
                        HttpStatus.FORBIDDEN);
        ;

        String code;
        String message;
        HttpStatus httpStatus;

        @Override
        public String getCode() {
                return code;
        }

        @Override
        public String getMessage() {
                return message;
        }

        @Override
        public HttpStatus getHttpStatus() {
                return httpStatus;
        }
}
