package com.uniwise.common.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
public class UpdatePriceTierRequest {

    @Size(max = 255, message = "TIER_NAME_INVALID")
    private String tierName;

    @DecimalMin(value = "0.00", inclusive = true, message = "PRICE_AMOUNT_INVALID")
    private BigDecimal priceAmount;

    @Size(max = 10, message = "CURRENCY_INVALID")
    private String currency;
}