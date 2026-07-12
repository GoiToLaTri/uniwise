package com.uniwise.course_service.modules.course_mgmt.section.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;

public interface SectionRepository extends JpaRepository<Section, String> {

    Optional<Section> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    boolean existsByCourseIdAndSortOrder(String courseId, Integer sortOrder);

    boolean existsByCourseIdAndSortOrderAndPublicIdNot(String courseId, Integer sortOrder, String publicId);

    @Query("SELECT COALESCE(MAX(s.sortOrder), 0) FROM Section s WHERE s.course.id = :courseId")
    Integer findMaxSortOrderByCourseId(@Param("courseId") String courseId);

    long countByCourseId(String courseId);

    @Modifying
    @Query("UPDATE Section s SET s.sortOrder = s.sortOrder + 1 WHERE s.course.id = :courseId AND s.sortOrder >= :sortOrder")
    void shiftSortOrderUp(@Param("courseId") String courseId, @Param("sortOrder") Integer sortOrder);

    @Modifying
    @Query("UPDATE Section s SET s.sortOrder = s.sortOrder - 1 WHERE s.course.id = :courseId AND s.sortOrder > :sortOrder")
    void shiftSortOrderDown(@Param("courseId") String courseId, @Param("sortOrder") Integer sortOrder);

    @Modifying
    @Query("UPDATE Section s SET s.sortOrder = s.sortOrder - 1 WHERE s.course.id = :courseId AND s.sortOrder > :oldOrder AND s.sortOrder <= :newOrder")
    void shiftSortOrderRangeDown(@Param("courseId") String courseId, @Param("oldOrder") Integer oldOrder, @Param("newOrder") Integer newOrder);

    @Modifying
    @Query("UPDATE Section s SET s.sortOrder = s.sortOrder + 1 WHERE s.course.id = :courseId AND s.sortOrder >= :newOrder AND s.sortOrder < :oldOrder")
    void shiftSortOrderRangeUp(@Param("courseId") String courseId, @Param("newOrder") Integer newOrder, @Param("oldOrder") Integer oldOrder);

    @Query("SELECT s FROM Section s WHERE " +
           "(:courseId IS NULL OR s.course.id = :courseId) " +
           "AND (:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Section> searchSections(
            @Param("courseId") String courseId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
