package com.uniwise.common.dto.response;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    String id;
    String accountId;
    String courseId;
    Long amount;
    String currency;
    String status;
    String txnRef;
    String paymentUrl;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
