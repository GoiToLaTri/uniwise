package com.uniwise.common.dto.request;

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
public class HashtagUpdateRequest {
 
    @Size(min = 1, max = 100, message = "HASHTAG_NAME_TOO_LONG")
    String name;
 
    // NOTE: isVerified chỉ thay đổi qua toggleVerified() - không update trực tiếp
    // NOTE: courseCount là read-only - không update trực tiếp
}