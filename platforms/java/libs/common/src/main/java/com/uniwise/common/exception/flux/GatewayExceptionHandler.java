package com.uniwise.common.exception.flux;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniwise.common.dto.response.ErrorResponse;
import com.uniwise.common.exception.GlobalExceptionHandler;
import com.uniwise.common.exception.errors.ErrorDefinition;
import com.uniwise.common.exception.errors.GatewayError;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnMissingBean(GlobalExceptionHandler.class)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler{
    ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted())
            return Mono.error(ex);

        // 1. Khởi tạo mặc định bằng Enum
        ErrorDefinition errorDef = GatewayError.INTERNAL_SERVER_ERROR;
        String dynamicMessage = null; // Dùng để lưu reason từ ResponseStatusException nếu có
        // Phân loại lỗi tại Gateway

        if (ex instanceof ResponseStatusException rsEx) {
            errorDef = GatewayError.GATEWAY_PROCESSING_ERROR;
            dynamicMessage = rsEx.getReason();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused"))
            errorDef = GatewayError.SERVICE_UNAVAILABLE;

        // 3. Thiết lập Response
        HttpStatusCode status = (ex instanceof ResponseStatusException rsEx) 
                                ? rsEx.getStatusCode() 
                                : errorDef.getHttpStatus();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 4. Build ErrorResponse (Ưu tiên message từ rsEx nếu có, không thì lấy từ Enum)
        ErrorResponse errorDetails = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(errorDef.getHttpStatus().value())
                .code(errorDef.getCode())
                .detail(dynamicMessage != null ? dynamicMessage : errorDef.getMessage())
                .path(exchange.getRequest().getPath().value())
                .build();

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                // Ép kiểu ErrorResponse thành JSON byte array
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(errorDetails));
            } catch (JsonProcessingException e) {
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
