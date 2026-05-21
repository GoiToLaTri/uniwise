package com.uniwise.user_service.modules.instructor.entity;

import java.time.LocalDate;

import com.uniwise.user_service.modules.instructor.enums.EInstructorCredentialType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "instructor_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DegreeCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    InstructorProfile instructorProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    EInstructorCredentialType type;

    @Column(nullable = false, length = 255)
    String name;

    @Column(length = 255)
    String institution;

    @Column(name = "issued_date")
    LocalDate issuedDate;

    @Column(length = 1000)
    String description;

    @Column(name = "credential_url", length = 500)
    String credentialUrl;
}
