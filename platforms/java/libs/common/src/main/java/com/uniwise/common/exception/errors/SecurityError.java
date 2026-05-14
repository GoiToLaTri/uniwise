package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum SecurityError implements ErrorDefinition{
    UNAUTHORIZED(
        "SEC_001",
        "Authentication is required to access this resource.",
        HttpStatus.UNAUTHORIZED
    ),
    FORBIDDEN(
        "SEC_002",
        "You do not have permission to access this resource.",
        HttpStatus.FORBIDDEN
    );

    ;
    String code;
    String message;
    HttpStatus httpStatus;

    @Override
    public String getCode() { return code; }
    @Override
    public String getMessage() { return message; }
    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
}
