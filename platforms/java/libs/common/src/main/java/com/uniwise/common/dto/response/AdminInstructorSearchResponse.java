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
public class AdminInstructorSearchResponse {
    String applicationPublicId;
    String publicId;
    String name;
    String professionalName;
    String avatarUrl;
    String headline;
    String biography;
    Integer yearsOfExperience;
    String status;
    String reviewComment;
    String appliedAt;
    String approvedAt;
    String rejectedAt;
    String suspendedAt;
    String reactivatedAt;
    List<InstructorExpertiseResponse> expertises;
}
