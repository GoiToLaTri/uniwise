package com.uniwise.user_service.modules.profile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uniwise.user_service.modules.profile.entity.Profile;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    Optional<Profile> findByAccountId(String accountId);
    Optional<Profile> findByPublicId(String publicId);
    Optional<Profile> findByEmail(String email);

    boolean existsByAccountId(String accountId);
    boolean existsByEmail(String email);
    boolean existsByPublicId(String publicId);
}
