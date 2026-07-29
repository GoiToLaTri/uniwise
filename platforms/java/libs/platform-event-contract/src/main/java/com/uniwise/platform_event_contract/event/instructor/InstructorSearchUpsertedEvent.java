package com.uniwise.platform_event_contract.event.instructor;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorSearchUpsertedEvent {
    private String id;
    private String accountId;
    private String applicationPublicId;
    private String publicId;
    private String displayName;
    private String professionalName;
    private String avatarUrl;
    private String headline;
    private String biography;
    private Integer yearsOfExperience;
    private String status;
    private String reviewComment;
    private Instant appliedAt;
    private Instant approvedAt;
    private Instant rejectedAt;
    private Instant suspendedAt;
    private Instant reactivatedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<InstructorExpertiseSnapshot> expertises;
    private List<InstructorDegreeSnapshot> degrees;
}
