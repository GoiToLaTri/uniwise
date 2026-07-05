package com.uniwise.payment_service.modules.payment;

import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.utils.ServletUtils;
import com.uniwise.common.dto.request.PaymentCreateRequest;
import com.uniwise.common.dto.response.PaymentResponse;


import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {
    PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> create(@RequestBody @Valid PaymentCreateRequest request) {
        String accountId = SecurityContextHolder.getContext().getAuthentication().getName();
        String ipAddress = ServletUtils.getRemoteAddress();
        
        log.info("REST: Create payment request for accountId={}, courseId={}, ipAddress={}", 
                accountId, request.getCourseId(), ipAddress);
        
        PaymentResponse response = paymentService.createPayment(accountId, request.getCourseId(), ipAddress);
        return ApiResponse.<PaymentResponse>builder()
                .code("CREATED")
                .message("Payment url generated successfully")
                .data(response)
                .build();
    }

    @GetMapping("/vnpay-ipn")
    public String processIpn(@RequestParam Map<String, String> requestParams) {
        log.info("REST: Received VNPay IPN Callback");
        return paymentService.processIpn(requestParams);
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getById(@PathVariable String id) {
        log.info("REST: Get payment transaction by ID: {}", id);
        PaymentResponse response = paymentService.getPaymentById(id);
        return ApiResponse.<PaymentResponse>builder()
                .code("OK")
                .message("Payment transaction retrieved successfully")
                .data(response)
                .build();
    }
}
