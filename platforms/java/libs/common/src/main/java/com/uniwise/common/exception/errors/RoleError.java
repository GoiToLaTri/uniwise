package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum RoleError implements ErrorDefinition {
    ROLE_NOT_FOUND(
            "ROL_001",
            "Role not found",
            HttpStatus.NOT_FOUND),

    ROLE_ALREADY_EXISTS(
            "ROL_002",
            "Role already exists",
            HttpStatus.CONFLICT),

    ROLE_DELETE_FAILED(
            "ROL_003",
            "Failed to delete role",
            HttpStatus.BAD_REQUEST),

    SOME_PERMISSIONS_NOT_FOUND(
            "ROL_004",
            "Some permissions not found",
            HttpStatus.BAD_REQUEST);

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
