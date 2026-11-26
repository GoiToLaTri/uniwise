package com.uniwise.payment_service.modules.payment.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.PaymentError;
import com.uniwise.course.v1.CourseGrpcServiceGrpc.CourseGrpcServiceBlockingStub;
import com.uniwise.course.v1.GetCoursePriceRequest;
import com.uniwise.course.v1.GetCoursePriceResponse;
import com.uniwise.payment_service.modules.payment.entity.Payment;
import com.uniwise.payment_service.modules.payment.entity.PaymentStatus;
import com.uniwise.payment_service.modules.payment.mapper.PaymentMapper;
import com.uniwise.payment_service.modules.payment.repository.PaymentRepository;
import com.uniwise.payment_service.modules.payment.utils.VnPayUtils;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.payment.PaymentCompletedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final String TMN_CODE = "OQXNQEMM";
    private static final String HASH_SECRET = "TEST_SECRET_KEY_123456";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private CourseGrpcServiceBlockingStub courseServiceClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "courseServiceClient", courseServiceClient);
        ReflectionTestUtils.setField(paymentService, "vnpVersion", "2.1.0");
        ReflectionTestUtils.setField(paymentService, "vnpCommand", "pay");
        ReflectionTestUtils.setField(paymentService, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(paymentService, "vnpTmnCode", TMN_CODE);
        ReflectionTestUtils.setField(paymentService, "vnpHashSecret", HASH_SECRET);
        ReflectionTestUtils.setField(paymentService, "vnpReturnUrl", "http://localhost:3000/payment/result");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void createPayment_blocksDuplicateSuccessfulPayment() {
        when(paymentRepository.existsByAccountIdAndCourseIdAndStatus("acc-1", "course-1", PaymentStatus.SUCCESS))
                .thenReturn(true);

        HttpException ex = assertThrows(HttpException.class,
                () -> paymentService.createPayment("acc-1", "course-1", "127.0.0.1"));

        assertEquals(PaymentError.TRANSACTION_ALREADY_COMPLETED, ex.getError());
        verifyNoInteractions(courseServiceClient);
    }

    @Test
    void createPayment_blocksZeroPriceCourseAndUsesGrpcDeadline() {
        CourseGrpcServiceBlockingStub deadlineStub = mock(CourseGrpcServiceBlockingStub.class);
        when(paymentRepository.existsByAccountIdAndCourseIdAndStatus("acc-1", "course-1", PaymentStatus.SUCCESS))
                .thenReturn(false);
        when(courseServiceClient.withDeadlineAfter(3, TimeUnit.SECONDS)).thenReturn(deadlineStub);
        when(deadlineStub.getCoursePrice(any(GetCoursePriceRequest.class))).thenReturn(
                GetCoursePriceResponse.newBuilder()
                        .setCourseId("course-1")
                        .setIsActive(true)
                        .setPrice(0L)
                        .setTitle("Free Course")
                        .build());

        HttpException ex = assertThrows(HttpException.class,
                () -> paymentService.createPayment("acc-1", "course-1", "127.0.0.1"));

        assertEquals(PaymentError.COURSE_NOT_ACTIVE, ex.getError());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processIpn_rejectsMismatchedTmnCodeAndBlankSignature() {
        Map<String, String> blankSig = new HashMap<>();
        blankSig.put("vnp_TxnRef", "TXN_1");
        blankSig.put("vnp_SecureHash", "");
        assertTrue(paymentService.processIpn(blankSig).contains("\"RspCode\":\"97\""));

        Map<String, String> wrongTmn = signedIpnParams("WRONG_TMN", "TXN_1", "1000000", "00", "00");
        assertTrue(paymentService.processIpn(wrongTmn).contains("\"RspCode\":\"97\""));
        verifyNoInteractions(paymentRepository, eventPublisher);
    }

    @Test
    void processIpn_rejectsSubHundredRemainderAndNonNumericAmount() {
        Payment pending = Payment.builder()
                .id("pay-1")
                .txnRef("TXN_1")
                .amount(10000L)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByTxnRefForUpdate("TXN_1")).thenReturn(Optional.of(pending));

        Map<String, String> truncatedTrap = signedIpnParams(TMN_CODE, "TXN_1", "1000099", "00", "00");
        assertTrue(paymentService.processIpn(truncatedTrap).contains("\"RspCode\":\"04\""));

        Map<String, String> nonNumeric = signedIpnParams(TMN_CODE, "TXN_1", "abc", "00", "00");
        assertTrue(paymentService.processIpn(nonNumeric).contains("\"RspCode\":\"04\""));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void processIpn_locksTransactionAndDefersPaymentCompletedEventAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        Payment pending = Payment.builder()
                .id("pay-1")
                .accountId("acc-1")
                .courseId("course-1")
                .txnRef("TXN_1")
                .amount(250000L)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByTxnRefForUpdate("TXN_1")).thenReturn(Optional.of(pending));

        Map<String, String> validIpn = signedIpnParams(TMN_CODE, "TXN_1", "25000000", "00", "00");
        String response = paymentService.processIpn(validIpn);

        assertTrue(response.contains("\"RspCode\":\"00\""));
        assertEquals(PaymentStatus.SUCCESS, pending.getStatus());
        verify(paymentRepository).findByTxnRefForUpdate("TXN_1");
        verify(eventPublisher, never()).publish(eq(Exchanges.EVENTS), eq(RoutingKeys.PAYMENT_COMPLETED), any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        verify(eventPublisher).publish(
                eq(Exchanges.EVENTS),
                eq(RoutingKeys.PAYMENT_COMPLETED),
                any(PaymentCompletedEvent.class));
    }

    @Test
    void processIpn_persistsMetadataOnFailedPayment() {
        Payment pending = Payment.builder()
                .id("pay-1")
                .txnRef("TXN_2")
                .amount(100000L)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByTxnRefForUpdate("TXN_2")).thenReturn(Optional.of(pending));

        Map<String, String> failedIpn = signedIpnParams(TMN_CODE, "TXN_2", "10000000", "24", "02");
        String response = paymentService.processIpn(failedIpn);

        assertTrue(response.contains("\"RspCode\":\"00\""));
        assertEquals(PaymentStatus.FAILED, pending.getStatus());
        assertEquals("14567890", pending.getVnpayTransactionNo());
        assertEquals("NCB", pending.getBankCode());
        assertEquals("20261126204000", pending.getPayDate());
        verifyNoInteractions(eventPublisher);
    }

    private Map<String, String> signedIpnParams(
            String tmnCode, String txnRef, String amount, String responseCode, String txnStatus) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", txnStatus);
        params.put("vnp_TransactionNo", "14567890");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_PayDate", "20261126204000");

        String hashData = VnPayUtils.buildHashData(params);
        params.put("vnp_SecureHash", VnPayUtils.hmacSHA512(HASH_SECRET, hashData));
        return params;
    }
}
