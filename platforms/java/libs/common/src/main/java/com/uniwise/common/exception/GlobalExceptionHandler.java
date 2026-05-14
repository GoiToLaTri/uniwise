package com.uniwise.common.exception;

import java.time.Instant;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.uniwise.common.dto.response.ErrorResponse;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.ErrorDefinition;
import com.uniwise.common.exception.errors.ServerError;
import com.uniwise.common.exception.errors.ValidationError;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnMissingBean(GlobalExceptionHandler.class) // Chỉ tạo nếu chưa có ai tạo Handler này
@Order(Ordered.LOWEST_PRECEDENCE) // Ưu tiên thấp nhất để nhường chỗ cho App chính
@Slf4j
public class GlobalExceptionHandler {
    // Bắt tất cả lỗi chưa được xử lý, trả về lỗi Server Error chung
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ErrorResponse> exceptionHandling(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception: {}", exception.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .status(ServerError.SERVER_ERROR.getHttpStatus().value())
                .code(ServerError.SERVER_ERROR.getCode())
                .detail(ServerError.SERVER_ERROR.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(ServerError.SERVER_ERROR.getHttpStatus()).body(response);
    }

    // Bắt lỗi từ HttpException và các subclass của nó, trả về lỗi theo định nghĩa
    // trong ErrorDefinition
    @ExceptionHandler(value = HttpException.class)
    ResponseEntity<ErrorResponse> runtimeExceptionHandling(HttpException exception, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .status(exception.getError().getHttpStatus().value())
                .code(exception.getError().getCode())
                .detail(exception.getError().getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(exception.getError().getHttpStatus().value()).body(response);
    }

    // Bắt lỗi validation từ @Valid, @Validated
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validationExceptionHandling(MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String enumKey = Objects.requireNonNull(exception.getFieldError()).getDefaultMessage();
        ErrorDefinition validationError = ValidationError.KEY_INVALID;

        try {
            validationError = ValidationError.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
        }

        ErrorResponse response = ErrorResponse.builder()
                .status(validationError.getHttpStatus().value())
                .code(validationError.getCode())
                .detail(validationError.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(validationError.getHttpStatus().value()).body(response);
    }

    // Bắt lỗi khi request body không đọc được (ví dụ JSON sai định dạng), trả về lỗi INVALID_REQUEST_BODY
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        ErrorDefinition error = ValidationError.INVALID_REQUEST_BODY;

        ErrorResponse response = ErrorResponse.builder()
                .status(error.getHttpStatus().value())
                .code(error.getCode())
                .detail(error.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(error.getHttpStatus().value()).body(response);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        ErrorDefinition error = AuthError.ACCESS_DENIED;

        ErrorResponse response = ErrorResponse.builder()
                .status(error.getHttpStatus().value())
                .code(error.getCode())
                .detail(error.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(error.getHttpStatus().value()).body(response);
    }
}
