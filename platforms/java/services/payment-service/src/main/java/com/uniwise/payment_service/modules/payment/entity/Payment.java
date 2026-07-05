package com.uniwise.payment_service.modules.payment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "account_id", nullable = false, length = 36)
    String accountId;

    @Column(name = "course_id", nullable = false, length = 36)
    String courseId;

    @Column(nullable = false)
    Long amount;

    @Builder.Default
    @Column(nullable = false, length = 10)
    String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentStatus status;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 100)
    String txnRef;

    @Column(name = "vnpay_transaction_no", length = 100)
    String vnpayTransactionNo;

    @Column(name = "bank_code", length = 50)
    String bankCode;

    @Column(name = "pay_date", length = 50)
    String payDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
