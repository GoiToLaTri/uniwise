package com.uniwise.user_service.modules.profile.entity;

import com.uniwise.user_service.modules.profile.enums.ProfileType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, unique = true, length = 100)
    String email;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "account_id", nullable = false, unique = true)
    String accountId;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "bio", length = 500)
    String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false, length = 50)
    ProfileType profileType;

    @Column(name = "public_id", unique = true, length = 100)
    String publicId;

}
