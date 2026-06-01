package com.uniwise.common.dto.response;

import java.util.List;

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
public class InstructorProfileResponse {
    String id;
    String accountId;
    String publicId;
    String name;
    String headline;
    String biography;
    Integer yearsOfExperience;
    String status;
    String reviewComment;
    String appliedAt;
    String approvedAt;
    String rejectedAt;
    List<DegreeDto> degrees;
    List<ExpertiseDto> expertises;
}
