package com.uniwise.common.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyncVideoPositionRequest {
    
    @NotNull(message = "LAST_WATCHED_POSITION_REQUIRED")
    @Min(value = 0, message = "LAST_WATCHED_POSITION_INVALID")
    Integer lastWatchedPosition;
}
