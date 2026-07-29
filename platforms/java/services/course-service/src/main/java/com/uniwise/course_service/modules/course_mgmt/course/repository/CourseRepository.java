package com.uniwise.course_service.modules.course_mgmt.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;

import jakarta.transaction.Transactional;

public interface CourseRepository extends JpaRepository<Course, String> {

    Optional<Course> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    @EntityGraph(attributePaths = "priceTier")
    List<Course> findAllByIsActiveTrue();

    @EntityGraph(attributePaths = "priceTier")
    List<Course> findAllByCreatorIdAndIsActiveTrue(String creatorId);

    @Query("SELECT c FROM Course c WHERE " +
            "c.isActive = true " +
            "AND (:creatorId IS NULL OR c.creatorId = :creatorId) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchCourses(
            @Param("creatorId") String creatorId,
            @Param("status") ECourseStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE courses SET student_count = student_count + 1 WHERE id = :courseId", nativeQuery = true)
    void incrementStudentCount(@Param("courseId") String courseId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE courses SET average_rating = :averageRating, total_reviews = :totalReviews WHERE id = :courseId", nativeQuery = true)
    void updateCourseRating(@Param("courseId") String courseId, @Param("averageRating") Double averageRating, @Param("totalReviews") Integer totalReviews);

    @Modifying
    @Transactional
    @Query(value = "UPDATE courses SET total_sections = total_sections + 1 WHERE id = :courseId", nativeQuery = true)
    void incrementTotalSections(@Param("courseId") String courseId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE courses SET total_sections = GREATEST(total_sections - 1, 0) WHERE id = :courseId", nativeQuery = true)
    void decrementTotalSections(@Param("courseId") String courseId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE courses SET total_lessons = total_lessons + 1 WHERE id = :courseId", nativeQuery = true)
    void incrementTotalLessons(@Param("courseId") String courseId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE courses SET total_lessons = GREATEST(total_lessons - 1, 0) WHERE id = :courseId", nativeQuery = true)
    void decrementTotalLessons(@Param("courseId") String courseId);
}
