package com.uniwise.course_service.modules.hashtag.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.hashtag.entity.Hashtag;

public interface HashtagRepository extends JpaRepository<Hashtag, String> {
 
    // ===== BASIC QUERIES =====
    Optional<Hashtag> findByName(String name);
 
    // ===== EXISTENCE CHECKS =====
    boolean existsByName(String name);
 
    boolean existsByNameAndIdNot(String name, String id);
 
    // ===== CUSTOM SEARCH QUERY =====
    @Query("SELECT h FROM Hashtag h WHERE " +
           "(:keyword IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isVerified IS NULL OR h.isVerified = :isVerified)")
    Page<Hashtag> searchHashtags(
            @Param("keyword") String keyword,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable
    );
 
    // ===== COUNTER UPDATES (atomic, avoid optimistic lock issues) =====
    @Modifying
    @Query("UPDATE Hashtag h SET h.courseCount = h.courseCount + 1 WHERE h.id = :id")
    void incrementCourseCount(@Param("id") String id);
 
    @Modifying
    @Query("UPDATE Hashtag h SET h.courseCount = GREATEST(h.courseCount - 1, 0) WHERE h.id = :id")
    void decrementCourseCount(@Param("id") String id);
}
