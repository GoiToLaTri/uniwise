package com.uniwise.course_service.modules.course_mgmt.course.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;

public interface CourseRepository extends JpaRepository<Course, String> {

    java.util.Optional<Course> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    @Query("SELECT c FROM Course c WHERE " +
           "c.isActive = true " +
           "AND (:creatorId IS NULL OR c.creatorId = :creatorId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchCourses(
            @Param("creatorId") String creatorId,
            @Param("status") com.uniwise.common.enums.ECourseStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
