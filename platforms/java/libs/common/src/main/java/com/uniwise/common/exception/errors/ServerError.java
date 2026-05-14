package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum ServerError implements ErrorDefinition{
    SERVER_ERROR("SV_001","Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
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
