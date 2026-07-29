package com.uniwise.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorSummaryResponse {
    private String publicId;
    private String name;
    private String avatarUrl;
}
