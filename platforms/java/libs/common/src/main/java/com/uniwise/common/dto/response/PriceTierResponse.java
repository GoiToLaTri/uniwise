package com.uniwise.common.dto.response;

import java.math.BigDecimal;
// import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceTierResponse {

    private String id;
    private String tierName;
    private BigDecimal priceAmount;
    private String currency;

    /** Number of courses currently using this tier (populated on demand). */
    private Integer courseCount;

    // private Instant createdAt;
    // private Instant updatedAt;
}
