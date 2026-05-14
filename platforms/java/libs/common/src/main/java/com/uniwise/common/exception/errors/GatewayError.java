package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum GatewayError implements ErrorDefinition{
    INTERNAL_SERVER_ERROR(
        "GW_001",
        "An unexpected error occurred.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    GATEWAY_PROCESSING_ERROR(
        "GW_002",
        "Gateway processing failed.",
        HttpStatus.BAD_GATEWAY
    ),
    SERVICE_UNAVAILABLE(
        "GW_003",
        "Service is currently unavailable. Please try again later.",
        HttpStatus.SERVICE_UNAVAILABLE
    )
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
