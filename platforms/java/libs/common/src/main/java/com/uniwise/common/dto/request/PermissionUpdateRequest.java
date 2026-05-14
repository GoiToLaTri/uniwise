package com.uniwise.common.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionUpdateRequest {
    @Size(min = 3, max = 50, message = "PERMISSION_NAME_INVALID")
    String name;

    @Size(max = 255, message = "PERMISSION_DESCRIPTION_TOO_LONG")
    String description;
}
