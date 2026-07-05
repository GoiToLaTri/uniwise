package com.uniwise.platform_event_contract.event.payment;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private String paymentId;
    private String accountId;
    private String courseId;
    private Long amount;
    private Instant completedAt;
}
