package com.uniwise.user_service.modules.instructor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;


public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, String> {
    @Query("SELECT i FROM InstructorProfile i WHERE i.profile.accountId = :accountId")
    Optional<InstructorProfile> findByAccountId(@Param("accountId") String accountId);

    @EntityGraph(attributePaths = { "profile", "expertises" })
    @Query("""
            SELECT i
            FROM InstructorProfile i
            WHERE i.profile.publicId = :publicId
              AND i.status = :status
            """)
    Optional<InstructorProfile> findByProfilePublicIdAndStatus(
            @Param("publicId") String publicId,
            @Param("status") EInstructorProfileStatus status);
    
    Optional<InstructorProfile> findByPublicId(String publicId);

    @Query("SELECT COUNT(i) > 0 FROM InstructorProfile i WHERE i.profile.accountId = :accountId")
    boolean existsByAccountId(@Param("accountId") String accountId);

    boolean existsByPublicId(String publicId);
    
    List<InstructorProfile> findAllByStatus(EInstructorProfileStatus status);

    @Query("SELECT i FROM InstructorProfile i WHERE (:status IS NULL OR :status = '' OR i.status = :status)")
    Page<InstructorProfile> findAllByStatus(@Param("status") EInstructorProfileStatus status, Pageable pageable);

    @Query(
            value = "SELECT i.id FROM InstructorProfile i",
            countQuery = "SELECT COUNT(i) FROM InstructorProfile i")
    Page<String> findIdsForSearchReindex(Pageable pageable);

    @EntityGraph(attributePaths = { "profile", "degrees", "expertises" })
    @Query("SELECT DISTINCT i FROM InstructorProfile i WHERE i.id IN :ids")
    List<InstructorProfile> findAllForSearchReindex(@Param("ids") List<String> ids);
}
