package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum CourseError implements ErrorDefinition {
    COURSE_NOT_FOUND("E_COURSE_001", "Course not found", HttpStatus.NOT_FOUND),
    PRICE_TIER_NOT_FOUND("E_COURSE_002", "Price tier not found", HttpStatus.NOT_FOUND),
    COURSE_NOT_FREE("E_COURSE_003", "This course is not free and requires payment", HttpStatus.BAD_REQUEST),
    USER_NOT_ENROLLED("E_COURSE_004", "User is not enrolled in this course", HttpStatus.FORBIDDEN),
    INSTRUCTOR_PROFILE_NOT_FOUND(
            "E_COURSE_005",
            "Instructor profile not found",
            HttpStatus.UNPROCESSABLE_ENTITY),
    INSTRUCTOR_PROFILE_SERVICE_UNAVAILABLE(
            "E_COURSE_006",
            "Instructor profile service is unavailable",
            HttpStatus.SERVICE_UNAVAILABLE);

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
