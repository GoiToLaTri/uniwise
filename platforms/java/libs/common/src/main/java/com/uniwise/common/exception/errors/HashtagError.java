package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum HashtagError implements ErrorDefinition{
    HASHTAG_NOT_FOUND("EHASHTAG_001", "Hashtag not found", HttpStatus.NOT_FOUND),
    HASHTAG_ALREADY_EXISTS("EHASHTAG_002", "Hashtag already exists", HttpStatus.CONFLICT),
    HASHTAG_INVALID_INPUT("EHASHTAG_003", "Invalid input data", HttpStatus.BAD_REQUEST);

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
