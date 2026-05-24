package com.uniwise.common.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class RoleCreateRequest {
    @NotBlank(message = "DISPLAY_NAME_REQUIRED")
    @Size(min = 1, max = 50, message = "DISPLAY_NAME_INVALID")
    String displayName;

    @NotBlank(message = "NAME_REQUIRED")
    @Size(min = 1, max = 50, message = "NAME_INVALID")
    String name;

    @Size(max = 255, message = "DESCRIPTION_INVALID")
    String description;

    @Builder.Default
    Boolean isActive = true;
}
