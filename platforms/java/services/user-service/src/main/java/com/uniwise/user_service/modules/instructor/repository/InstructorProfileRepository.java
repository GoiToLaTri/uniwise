package com.uniwise.user_service.modules.instructor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;

@Repository
public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, String> {
    Optional<InstructorProfile> findByAccountId(String accountId);
    Optional<InstructorProfile> findByPublicId(String publicId);
    boolean existsByAccountId(String accountId);
    boolean existsByPublicId(String publicId);
    List<InstructorProfile> findAllByStatus(EInstructorProfileStatus status);
}
