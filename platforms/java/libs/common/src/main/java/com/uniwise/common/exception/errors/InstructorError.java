package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum InstructorError implements ErrorDefinition {
    INSTRUCTOR_PROFILE_NOT_FOUND("INS_001", "Instructor profile not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_PROFILE_ALREADY_EXISTS("INS_002", "Instructor profile already exists for this account", HttpStatus.CONFLICT),
    PUBLIC_ID_ALREADY_EXISTS("INS_003", "Public ID already exists", HttpStatus.CONFLICT),
    USER_PROFILE_NOT_FOUND("INS_004", "User profile not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_PROFILE_NOT_APPROVED("INS_005", "Instructor profile is not approved", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED("INS_006", "Authentication required", HttpStatus.UNAUTHORIZED),
    INSTRUCTOR_PROFILE_ALREADY_APPROVED("INS_007", "Instructor profile is already approved", HttpStatus.BAD_REQUEST),
    INSTRUCTOR_PROFILE_ALREADY_REJECTED("INS_008", "Instructor profile is already rejected", HttpStatus.BAD_REQUEST),
    INSTRUCTOR_PROFILE_ALREADY_SUSPENDED("INS_009", "Instructor profile is already suspended", HttpStatus.BAD_REQUEST),
    INSTRUCTOR_PROFILE_ALREADY_REACTIVATED("INS_010", "Instructor profile is already reactivated", HttpStatus.BAD_REQUEST)
    ;

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
