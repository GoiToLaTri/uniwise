package com.uniwise.user_service.modules.instructor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;


public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, String> {
    Optional<InstructorProfile> findByAccountId(String accountId);
    
    Optional<InstructorProfile> findByPublicId(String publicId);

    boolean existsByAccountId(String accountId);

    boolean existsByPublicId(String publicId);
    
    List<InstructorProfile> findAllByStatus(EInstructorProfileStatus status);

    @Query("SELECT i FROM InstructorProfile i WHERE (:status IS NULL OR :status = '' OR i.status = :status)")
    Page<InstructorProfile> findAllByStatus(@Param("status") EInstructorProfileStatus status, Pageable pageable);
}
