package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum ValidationError implements ErrorDefinition {
        KEY_INVALID(
                        "VAL_001",
                        "Key invalid",
                        HttpStatus.BAD_REQUEST),
        NAME_REQUIRED("VAL_002", "Name is required", HttpStatus.BAD_REQUEST),
        NAME_INVALID("VAL_003", "Name is invalid", HttpStatus.BAD_REQUEST),

        PASSWORD_REQUIRED("VAL_004", "Password is required", HttpStatus.BAD_REQUEST),
        PASSWORD_INVALID("VAL_005", "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),

        EMAIL_REQUIRED("VAL_006", "Email is required", HttpStatus.BAD_REQUEST),
        EMAIL_INVALID("VAL_007", "Email is invalid",
                        HttpStatus.BAD_REQUEST),
        INVALID_REQUEST_BODY("VAL_008", "Invalid request body", HttpStatus.BAD_REQUEST),
        DESCRIPTION_INVALID("VAL_009", "Description must be between 1 and 255 characters", HttpStatus.BAD_REQUEST),
        BIO_INVALID("VAL_010", "Bio must not exceed 500 characters", HttpStatus.BAD_REQUEST),
        AVATAR_URL_INVALID("VAL_011", "Avatar URL must not exceed 255 characters", HttpStatus.BAD_REQUEST),
        PUBLIC_ID_INVALID("VAL_012", "Public ID must not exceed 100 characters", HttpStatus.BAD_REQUEST),
        REFRESH_TOKEN_REQUIRED(
                        "VAL_013",
                        "Refresh token is required",
                        HttpStatus.BAD_REQUEST),
        PERMISSION_NAME_REQUIRED(
                        "VAL_001",
                        "Permission name is required",
                        HttpStatus.BAD_REQUEST),
        PERMISSION_NAME_INVALID(
                        "VAL_002",
                        "Permission name must be between 3 and 50 characters",
                        HttpStatus.BAD_REQUEST),
        PERMISSION_DESCRIPTION_TOO_LONG(
                        "VAL_003",
                        "Permission description must not exceed 255 characters",
                        HttpStatus.BAD_REQUEST),
        DEGREE_TYPE_INVALID(
                        "VAL_014",
                        "Degree type must be one of the following: Bachelor's, Master's, PhD",
                        HttpStatus.BAD_REQUEST),
        DISPLAY_NAME_REQUIRED(
                        "VAL_015",
                        "Display name is required",
                        HttpStatus.BAD_REQUEST),
        DISPLAY_NAME_INVALID(
                        "VAL_016",
                        "Display name must be between 1 and 50 characters",
                        HttpStatus.BAD_REQUEST),
        TIER_NAME_REQUIRED(
                        "PT_VAL_001",
                        "Tier name is required",
                        HttpStatus.BAD_REQUEST),

        TIER_NAME_INVALID(
                        "PT_VAL_002",
                        "Tier name must not exceed 255 characters",
                        HttpStatus.BAD_REQUEST),

        PRICE_AMOUNT_REQUIRED(
                        "PT_VAL_003",
                        "Price amount is required",
                        HttpStatus.BAD_REQUEST),

        PRICE_AMOUNT_INVALID(
                        "PT_VAL_004",
                        "Price amount must be zero or positive",
                        HttpStatus.BAD_REQUEST),

        CURRENCY_REQUIRED(
                        "PT_VAL_005",
                        "Currency is required",
                        HttpStatus.BAD_REQUEST),

        CURRENCY_INVALID(
                        "PT_VAL_006",
                        "Currency code must not exceed 10 characters",
                        HttpStatus.BAD_REQUEST),

        HASHTAG_NAME_REQUIRED(
                        "HT_VAL_001",
                        "Hashtag name is required",
                        HttpStatus.BAD_REQUEST),

        HASHTAG_NAME_TOO_SHORT(
                        "HT_VAL_002",
                        "Hashtag name must be at least 1 character",
                        HttpStatus.BAD_REQUEST),

        HASHTAG_NAME_TOO_LONG(
                        "HT_VAL_003",
                        "Hashtag name must not exceed 100 characters",
                        HttpStatus.BAD_REQUEST);
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
