package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum AccountError implements ErrorDefinition {
    ACCOUNT_NOT_FOUND(
            "ACT_001",
            "Account not found",
            HttpStatus.NOT_FOUND),

    ACCOUNT_ALREADY_EXISTS(
            "ACT_002",
            "Account already exists",
            HttpStatus.CONFLICT),

    ACCOUNT_IN_PROGRESS("ACT_003",
            "Account registration is in progress",
            HttpStatus.BAD_REQUEST),

    DEFAULT_ROLES_NOT_FOUND(
            "ACT_004",
            "Default roles not found",
            HttpStatus.INTERNAL_SERVER_ERROR);

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
