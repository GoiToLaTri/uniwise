package com.uniwise.payment_service.modules.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uniwise.payment_service.modules.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByTxnRef(String txnRef);
    List<Payment> findByAccountId(String accountId);
}
