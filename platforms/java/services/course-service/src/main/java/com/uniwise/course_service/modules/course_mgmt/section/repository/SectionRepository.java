package com.uniwise.course_service.modules.course_mgmt.section.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;

public interface SectionRepository extends JpaRepository<Section, String> {

    java.util.Optional<Section> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    boolean existsByCourseIdAndSortOrder(String courseId, Integer sortOrder);

    boolean existsByCourseIdAndSortOrderAndPublicIdNot(String courseId, Integer sortOrder, String publicId);

    @Query("SELECT s FROM Section s WHERE " +
           "(:courseId IS NULL OR s.course.id = :courseId) " +
           "AND (:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Section> searchSections(
            @Param("courseId") String courseId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
