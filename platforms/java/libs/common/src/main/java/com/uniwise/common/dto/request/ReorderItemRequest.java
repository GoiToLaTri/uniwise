package com.uniwise.common.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ReorderItemRequest {

    @NotBlank(message = "ID_REQUIRED")
    String id;

    @NotNull(message = "SORT_ORDER_REQUIRED")
    @Min(value = 0, message = "SORT_ORDER_INVALID")
    Integer sortOrder;
}
