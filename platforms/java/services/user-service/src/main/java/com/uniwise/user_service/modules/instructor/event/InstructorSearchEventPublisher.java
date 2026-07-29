package com.uniwise.user_service.modules.instructor.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.instructor.InstructorDegreeSnapshot;
import com.uniwise.platform_event_contract.event.instructor.InstructorExpertiseSnapshot;
import com.uniwise.platform_event_contract.event.instructor.InstructorSearchUpsertedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;
import com.uniwise.user_service.modules.instructor.entity.DegreeCertificate;
import com.uniwise.user_service.modules.instructor.entity.Expertise;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.profile.entity.Profile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstructorSearchEventPublisher {

    private final EventPublisher eventPublisher;

    public void publish(InstructorProfile instructorProfile) {
        InstructorSearchUpsertedEvent event = toEvent(instructorProfile);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publishNow(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishNow(event);
            }
        });
    }

    private void publishNow(InstructorSearchUpsertedEvent event) {
        eventPublisher.publish(RoutingKeys.INSTRUCTOR_SEARCH_UPSERTED, event);
    }

    private InstructorSearchUpsertedEvent toEvent(InstructorProfile instructorProfile) {
        Profile profile = instructorProfile.getProfile();

        return InstructorSearchUpsertedEvent.builder()
                .id(instructorProfile.getId())
                .accountId(profile.getAccountId())
                .applicationPublicId(instructorProfile.getPublicId())
                .publicId(profile.getPublicId())
                .displayName(profile.getName())
                .professionalName(instructorProfile.getName())
                .avatarUrl(profile.getAvatarUrl())
                .headline(instructorProfile.getHeadline())
                .biography(instructorProfile.getBiography())
                .yearsOfExperience(instructorProfile.getYearsOfExperience())
                .status(instructorProfile.getStatus() == null ? null : instructorProfile.getStatus().name())
                .reviewComment(instructorProfile.getReviewComment())
                .appliedAt(toInstant(instructorProfile.getAppliedAt()))
                .approvedAt(toInstant(instructorProfile.getApprovedAt()))
                .rejectedAt(toInstant(instructorProfile.getRejectedAt()))
                .suspendedAt(toInstant(instructorProfile.getSuspendedAt()))
                .reactivatedAt(toInstant(instructorProfile.getReactivatedAt()))
                .createdAt(toInstant(instructorProfile.getCreatedAt()))
                .updatedAt(toInstant(instructorProfile.getUpdatedAt()))
                .expertises(toExpertiseSnapshots(instructorProfile))
                .degrees(toDegreeSnapshots(instructorProfile))
                .build();
    }

    private List<InstructorExpertiseSnapshot> toExpertiseSnapshots(InstructorProfile instructorProfile) {
        return instructorProfile.getExpertises().stream()
                .sorted(Comparator.comparing(
                        Expertise::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(expertise -> InstructorExpertiseSnapshot.builder()
                        .name(expertise.getName())
                        .description(expertise.getDescription())
                        .level(expertise.getLevel())
                        .build())
                .toList();
    }

    private List<InstructorDegreeSnapshot> toDegreeSnapshots(InstructorProfile instructorProfile) {
        return instructorProfile.getDegrees().stream()
                .sorted(Comparator.comparing(
                        DegreeCertificate::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(degree -> InstructorDegreeSnapshot.builder()
                        .type(degree.getType() == null ? null : degree.getType().name())
                        .name(degree.getName())
                        .institution(degree.getInstitution())
                        .issuedDate(degree.getIssuedDate())
                        .description(degree.getDescription())
                        .credentialUrl(degree.getCredentialUrl())
                        .build())
                .toList();
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
