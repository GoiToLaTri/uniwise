package com.uniwise.media_service.modules.upload.error;

import org.springframework.http.HttpStatus;

import com.uniwise.common.exception.errors.ErrorDefinition;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum UploadError implements ErrorDefinition {
    FILE_REQUIRED("UPL_001", "Upload file is required", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("UPL_002", "Upload file exceeds the allowed size", HttpStatus.CONTENT_TOO_LARGE),
    FILE_NAME_INVALID("UPL_003", "Upload file name or extension is invalid", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_ALLOWED("UPL_004", "Upload file type is not allowed", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    FILE_SIGNATURE_INVALID("UPL_005", "Upload file content does not match its declared type",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    FILE_UNREADABLE("UPL_006", "Upload file cannot be read", HttpStatus.BAD_REQUEST),
    LESSON_ID_REQUIRED("UPL_007", "Lesson ID is required", HttpStatus.BAD_REQUEST),
    LESSON_NOT_FOUND("UPL_008", "Upload target lesson was not found", HttpStatus.NOT_FOUND),
    LESSON_NOT_VIDEO("UPL_009", "Upload target lesson must be a video lesson", HttpStatus.BAD_REQUEST),
    LESSON_VALIDATION_UNAVAILABLE(
            "UPL_010",
            "Lesson upload authorization is temporarily unavailable",
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
