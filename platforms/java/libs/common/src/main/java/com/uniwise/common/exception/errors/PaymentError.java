package com.uniwise.common.exception.errors;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum PaymentError implements ErrorDefinition {
    PAYMENT_NOT_FOUND("PAY_001", "Payment transaction not found", HttpStatus.NOT_FOUND),
    COURSE_NOT_ACTIVE("PAY_002", "Course is not active for purchasing", HttpStatus.BAD_REQUEST),
    GRPC_ERROR("PAY_003", "Failed to communicate with course service", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_SIGNATURE("PAY_004", "Invalid checksum signature from VNPay", HttpStatus.BAD_REQUEST),
    TRANSACTION_ALREADY_COMPLETED("PAY_005", "Transaction is already completed", HttpStatus.BAD_REQUEST);

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
