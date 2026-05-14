package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum UserError implements ErrorDefinition{
    USER_NOT_FOUND(
        "USR_001",
        "User not found",
        HttpStatus.NOT_FOUND
    ),

    USER_ALREADY_EXISTS(
        "USR_002",
        "User already exists",
        HttpStatus.CONFLICT
    ),
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
