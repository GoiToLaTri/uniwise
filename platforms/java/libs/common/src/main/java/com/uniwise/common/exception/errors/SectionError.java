package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum SectionError implements ErrorDefinition {
    SECTION_NOT_FOUND("E_SECTION_001", "Section not found", HttpStatus.NOT_FOUND),
    COURSE_NOT_FOUND("E_SECTION_002", "Associated course not found", HttpStatus.NOT_FOUND),
    SECTION_SORT_ORDER_CONFLICT("E_SECTION_011", "Sort order conflict in this course", HttpStatus.CONFLICT);

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
