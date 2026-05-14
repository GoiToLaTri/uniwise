package com.uniwise.common.exception.errors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum PermissionError implements ErrorDefinition {
    PERMISSION_NOT_FOUND(
            "PERMISSION_001",
            "Permission with the specified id was not found.",
            HttpStatus.NOT_FOUND),
    PERMISSION_ALREADY_EXISTS(
            "PERMISSION_002",
            "Permission with the specified name already exists.",
            HttpStatus.CONFLICT),
    PERMISSION_NAME_INVALID(
            "PERMISSION_003",
            "Permission name is invalid or empty.",
            HttpStatus.BAD_REQUEST),
    PERMISSION_ID_INVALID(
            "PERMISSION_004",
            "Permission id is invalid.",
            HttpStatus.BAD_REQUEST),
    PERMISSION_DELETE_FAILED(
            "PERMISSION_005",
            "Failed to delete the permission.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    PERMISSION_SAVE_FAILED(
            "PERMISSION_006",
            "Failed to save the permission.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    PERMISSION_UPDATE_FAILED(
            "PERMISSION_007",
            "Failed to update the permission.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    PERMISSION_ACCESS_DENIED(
            "PERMISSION_008",
            "Access denied to the requested permission.",
            HttpStatus.FORBIDDEN);

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