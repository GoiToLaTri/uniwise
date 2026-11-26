package com.uniwise.payment_service.modules.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.payment_service.modules.payment.entity.Payment;
import com.uniwise.payment_service.modules.payment.entity.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByTxnRef(String txnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.txnRef = :txnRef")
    Optional<Payment> findByTxnRefForUpdate(@Param("txnRef") String txnRef);

    boolean existsByAccountIdAndCourseIdAndStatus(String accountId, String courseId, PaymentStatus status);

    List<Payment> findByAccountId(String accountId);
}
