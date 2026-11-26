package com.uniwise.payment_service.modules.payment.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.CourseError;
import com.uniwise.common.exception.errors.PaymentError;
import com.uniwise.course.v1.CourseGrpcServiceGrpc.CourseGrpcServiceBlockingStub;
import com.uniwise.course.v1.GetCoursePriceRequest;
import com.uniwise.course.v1.GetCoursePriceResponse;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;
import com.uniwise.payment_service.modules.payment.PaymentService;
import com.uniwise.common.dto.response.PaymentResponse;
import com.uniwise.payment_service.modules.payment.entity.Payment;
import com.uniwise.payment_service.modules.payment.entity.PaymentStatus;
import com.uniwise.payment_service.modules.payment.mapper.PaymentMapper;
import com.uniwise.payment_service.modules.payment.repository.PaymentRepository;
import com.uniwise.payment_service.modules.payment.utils.VnPayUtils;

import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.payment.PaymentCompletedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    PaymentMapper paymentMapper;
    EventPublisher eventPublisher;

    @NonFinal
    @GrpcClient("course-service")
    CourseGrpcServiceBlockingStub courseServiceClient;

    @NonFinal
    @Value("${vnpay.version}")
    String vnpVersion;

    @NonFinal
    @Value("${vnpay.command}")
    String vnpCommand;

    @NonFinal
    @Value("${vnpay.pay-url}")
    String vnpPayUrl;

    @NonFinal
    @Value("${vnpay.tmn-code}")
    String vnpTmnCode;

    @NonFinal
    @Value("${vnpay.hash-secret}")
    String vnpHashSecret;

    @NonFinal
    @Value("${vnpay.return-url}")
    String vnpReturnUrl;

    @Override
    @Transactional
    public PaymentResponse createPayment(String accountId, String courseId, String ipAddress) {
        log.info("Creating payment request for accountId: {}, courseId: {}", accountId, courseId);

        if (paymentRepository.existsByAccountIdAndCourseIdAndStatus(accountId, courseId, PaymentStatus.SUCCESS)) {
            throw new HttpException(PaymentError.TRANSACTION_ALREADY_COMPLETED);
        }

        // 1. Verify course existence and get price via gRPC
        GetCoursePriceResponse courseResponse;
        try {
            courseResponse = courseServiceClient
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getCoursePrice(
                            GetCoursePriceRequest.newBuilder().setCourseId(courseId).build());
        } catch (StatusRuntimeException e) {
            log.error("gRPC failure calling course-service", e);
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new HttpException(CourseError.COURSE_NOT_FOUND);
            }
            throw new HttpException(PaymentError.GRPC_ERROR);
        }

        if (!courseResponse.getIsActive()) {
            throw new HttpException(PaymentError.COURSE_NOT_ACTIVE);
        }

        long price = courseResponse.getPrice();
        if (price <= 0) {
            throw new HttpException(PaymentError.COURSE_NOT_ACTIVE);
        }

        // 2. Generate unique transaction reference code
        String txnRef = "TXN_" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);

        // 3. Save pending payment transaction in database
        Payment payment = Payment.builder()
                .accountId(accountId)
                .courseId(courseId)
                .amount(price)
                .currency("VND")
                .status(PaymentStatus.PENDING)
                .txnRef(txnRef)
                .build();

        Payment saved = paymentRepository.save(payment);

        // 4. Construct VNPay parameters map
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(price * 100)); // VNPay expects amount * 100
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan khoa hoc: " + courseResponse.getTitle());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Formats time in GMT+7 for VNPay
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", now.format(formatter));

        // 5. Generate secure hash and return payment redirection URL
        String query = VnPayUtils.buildQueryString(vnpParams);
        String vnpSecureHash = VnPayUtils.hmacSHA512(vnpHashSecret, query);
        // Thêm SecureHash vào cuối URL, KHÔNG thêm vào chuỗi hash
        String paymentUrl = vnpPayUrl + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;

        log.debug("[VNPAY DEBUG] Hash data (createPayment): {}", query);
        log.debug("[VNPAY DEBUG] Computed SecureHash: {}", vnpSecureHash);
        log.debug("Created payment URL for txnRef {}: {}", txnRef, paymentUrl);

        PaymentResponse response = paymentMapper.toResponse(saved);
        response.setPaymentUrl(paymentUrl);
        return response;
    }

    @Override
    @Transactional
    public String processIpn(Map<String, String> requestParams) {
        if (requestParams == null || requestParams.isEmpty()) {
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }
        log.info("Processing VNPay IPN Callback for txnRef: {}", requestParams.get("vnp_TxnRef"));

        // 1. Verify checksum signature (constant-time) and TMN code
        String vnpSecureHash = requestParams.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(requestParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String checkQuery = VnPayUtils.buildHashData(fields);
        log.debug("[VNPAY DEBUG] Received vnp_SecureHash: {}", vnpSecureHash);
        log.debug("[VNPAY DEBUG] Hash data (processIpn): {}", checkQuery);

        if (!VnPayUtils.verifySignature(vnpHashSecret, checkQuery, vnpSecureHash)) {
            log.error("VNPay Checksum verification failed!");
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }

        String receivedTmnCode = requestParams.get("vnp_TmnCode");
        if (!vnpTmnCode.equals(receivedTmnCode)) {
            log.error("VNPay TMN code mismatch! Expected: {}, Received: {}", vnpTmnCode, receivedTmnCode);
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }

        // 2. Fetch and lock pending transaction to serialize concurrent IPN callbacks
        String txnRef = requestParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }
        Payment payment = paymentRepository.findByTxnRefForUpdate(txnRef).orElse(null);
        if (payment == null) {
            log.error("Transaction not found for TxnRef: {}", txnRef);
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }

        // 3. Verify payment amount matches exactly (amount * 100 == vnp_Amount)
        String rawAmountStr = requestParams.get("vnp_Amount");
        long rawVnpAmount;
        try {
            if (rawAmountStr == null || rawAmountStr.isBlank()) {
                log.error("Missing vnp_Amount for TxnRef: {}", txnRef);
                return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
            }
            rawVnpAmount = Long.parseLong(rawAmountStr.trim());
        } catch (NumberFormatException e) {
            log.error("Invalid numeric vnp_Amount '{}' for TxnRef: {}", rawAmountStr, txnRef);
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
        }

        long expectedVnpAmount;
        try {
            expectedVnpAmount = Math.multiplyExact(payment.getAmount(), 100L);
        } catch (ArithmeticException e) {
            log.error("Payment amount overflow for TxnRef: {}", txnRef);
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
        }

        if (expectedVnpAmount != rawVnpAmount) {
            log.error("Amount mismatch! Expected (x100): {}, Received: {}", expectedVnpAmount, rawVnpAmount);
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
        }

        // 4. Verify transaction status
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Transaction already confirmed for TxnRef: {}, Status: {}", txnRef, payment.getStatus());
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        // 5. Update transaction status & persist VNPay metadata
        String responseCode = requestParams.get("vnp_ResponseCode");
        String transactionStatus = requestParams.get("vnp_TransactionStatus");
        payment.setVnpayTransactionNo(requestParams.get("vnp_TransactionNo"));
        payment.setBankCode(requestParams.get("vnp_BankCode"));
        payment.setPayDate(requestParams.get("vnp_PayDate"));

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            log.info("Payment SUCCESS for TxnRef: {}", txnRef);
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            // Publish message queue event after DB commit
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .accountId(payment.getAccountId())
                    .courseId(payment.getCourseId())
                    .amount(payment.getAmount())
                    .completedAt(Instant.now())
                    .build();
            publishPaymentCompletedAfterCommit(event);
        } else {
            log.warn("Payment FAILED for TxnRef: {}, responseCode: {}", txnRef, responseCode);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }

        return "{\"RspCode\":\"00\",\"Message\":\"Confirm success\"}";
    }

    private void publishPaymentCompletedAfterCommit(PaymentCompletedEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publish(Exchanges.EVENTS, RoutingKeys.PAYMENT_COMPLETED, event);
            log.info("Published PaymentCompletedEvent to RabbitMQ");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publish(Exchanges.EVENTS, RoutingKeys.PAYMENT_COMPLETED, event);
                log.info("Published PaymentCompletedEvent to RabbitMQ after commit");
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new HttpException(PaymentError.PAYMENT_NOT_FOUND));
        requireOwnerOrAdmin(payment.getAccountId());
        return paymentMapper.toResponse(payment);
    }

    private void requireOwnerOrAdmin(String ownerAccountId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        boolean isAdmin = isAuthenticated
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = isAuthenticated
                && ownerAccountId.equals(authentication.getName());

        if (!isOwner && !isAdmin)
            throw new HttpException(AuthError.ACCESS_DENIED);
    }
}
