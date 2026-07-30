package com.uniwise.search_service.modules.instructor.enums;

import java.util.Locale;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.InstructorError;

public enum InstructorSearchStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED;

    public static InstructorSearchStatus fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new HttpException(InstructorError.INSTRUCTOR_STATUS_INVALID);
        }
    }
}
