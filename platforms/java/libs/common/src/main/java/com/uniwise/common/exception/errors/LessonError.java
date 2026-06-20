package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum LessonError implements ErrorDefinition {
    LESSON_NOT_FOUND("E_LESSON_001", "Lesson not found", HttpStatus.NOT_FOUND),
    SECTION_NOT_FOUND("E_LESSON_002", "Section not found", HttpStatus.NOT_FOUND),
    LESSON_TYPE_INVALID("E_LESSON_021", "Invalid lesson type (must be VIDEO or QUIZ)", HttpStatus.BAD_REQUEST),
    LESSON_STATUS_INVALID("E_LESSON_022", "Invalid lesson status (must be PROCESSING or READY)", HttpStatus.BAD_REQUEST),
    LESSON_SORT_ORDER_CONFLICT("E_LESSON_011", "Sort order conflict in this section", HttpStatus.CONFLICT);

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
