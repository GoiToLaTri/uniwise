package com.uniwise.payment_service.modules.payment;

import java.util.Map;

import com.uniwise.common.dto.response.PaymentResponse;


public interface PaymentService {
    PaymentResponse createPayment(String accountId, String courseId, String ipAddress);
    String processIpn(Map<String, String> requestParams);
    PaymentResponse getPaymentById(String paymentId);
}
