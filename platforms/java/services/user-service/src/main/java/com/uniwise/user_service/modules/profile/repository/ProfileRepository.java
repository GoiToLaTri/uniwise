package com.uniwise.user_service.modules.profile.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uniwise.user_service.modules.profile.entity.Profile;
import com.uniwise.user_service.modules.profile.enums.ProfileType;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, String> {
    Optional<Profile> findByAccountId(String accountId);
    Optional<Profile> findByPublicId(String publicId);
    Optional<Profile> findByEmail(String email);
    @Query("SELECT p FROM Profile p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.publicId) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:profileType IS NULL OR p.profileType = :profileType)")
    Page<Profile> searchProfilesWithType(@Param("keyword") String keyword,
                                         @Param("profileType") ProfileType profileType,
                                         Pageable pageable);

    boolean existsByAccountId(String accountId);
    boolean existsByEmail(String email);
    boolean existsByPublicId(String publicId);
}
