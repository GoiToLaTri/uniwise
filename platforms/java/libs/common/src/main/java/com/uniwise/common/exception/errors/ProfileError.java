package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum ProfileError implements ErrorDefinition {
    PROFILE_NOT_FOUND("PRF_001", "Profile not found", HttpStatus.NOT_FOUND),
    PROFILE_ALREADY_EXISTS("PRF_002", "Profile already exists for this account", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("PRF_003", "Email address already exists", HttpStatus.CONFLICT),
    PUBLIC_ID_ALREADY_EXISTS("PRF_004", "Public ID already exists", HttpStatus.CONFLICT),
    UNAUTHENTICATED("PRF_005", "Authentication required", HttpStatus.UNAUTHORIZED),
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
