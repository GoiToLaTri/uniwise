package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum PriceTierError implements ErrorDefinition{
PRICE_TIER_NOT_FOUND(
            "PT_001",
            "Price tier not found",
            HttpStatus.NOT_FOUND
    ),

    PRICE_TIER_ALREADY_EXISTS(
            "PT_002",
            "Price tier with this name already exists",
            HttpStatus.CONFLICT
    ),

    PRICE_TIER_DELETE_FAILED(
            "PT_003",
            "Failed to delete price tier",
            HttpStatus.BAD_REQUEST
    ),

    PRICE_TIER_IN_USE(
            "PT_004",
            "Cannot delete price tier because it is currently assigned to one or more courses",
            HttpStatus.CONFLICT
    );

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
