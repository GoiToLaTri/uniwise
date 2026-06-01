package com.uniwise.user_service.modules.instructor.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "instructor_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "account_id", nullable = false, unique = true)
    String accountId;

    @Column(name = "public_id", nullable = false, unique = true, length = 100)
    String publicId;

    @Column(length = 150)
    String name;

    @Column(length = 150)
    String headline;

    @Column(length = 2000)
    String biography;

    @Column(name = "years_of_experience")
    Integer yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    EInstructorProfileStatus status;

    @Column(name = "review_comment", length = 1000)
    String reviewComment;

    @Column(name = "applied_at", updatable = false)
    LocalDateTime appliedAt;

    @Column(name = "approved_at")
    LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    LocalDateTime rejectedAt;

    @Column(name = "suspended_at")
    LocalDateTime suspendedAt;

    @Column(name = "reactivated_at")
    LocalDateTime reactivatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "instructorProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Set<DegreeCertificate> degrees = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "instructorProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Set<Expertise> expertises = new HashSet<>();
}
