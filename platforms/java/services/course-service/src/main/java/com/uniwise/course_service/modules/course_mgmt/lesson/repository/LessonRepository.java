package com.uniwise.course_service.modules.course_mgmt.lesson.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, String> {

    java.util.List<Lesson> findByContentReference(String contentReference);

    boolean existsBySectionIdAndSortOrder(String sectionId, Integer sortOrder);

    boolean existsBySectionIdAndSortOrderAndPublicIdNot(String sectionId, Integer sortOrder, String publicId);

    java.util.Optional<Lesson> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    @Query("SELECT COALESCE(MAX(l.sortOrder), 0) FROM Lesson l WHERE l.section.id = :sectionId")
    Integer findMaxSortOrderBySectionId(@Param("sectionId") String sectionId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Lesson l SET l.sortOrder = l.sortOrder + 1 WHERE l.section.id = :sectionId AND l.sortOrder >= :sortOrder")
    void shiftSortOrderUp(@Param("sectionId") String sectionId, @Param("sortOrder") Integer sortOrder);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Lesson l SET l.sortOrder = l.sortOrder - 1 WHERE l.section.id = :sectionId AND l.sortOrder > :sortOrder")
    void shiftSortOrderDown(@Param("sectionId") String sectionId, @Param("sortOrder") Integer sortOrder);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Lesson l SET l.sortOrder = l.sortOrder - 1 WHERE l.section.id = :sectionId AND l.sortOrder > :oldOrder AND l.sortOrder <= :newOrder")
    void shiftSortOrderRangeDown(@Param("sectionId") String sectionId, @Param("oldOrder") Integer oldOrder, @Param("newOrder") Integer newOrder);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Lesson l SET l.sortOrder = l.sortOrder + 1 WHERE l.section.id = :sectionId AND l.sortOrder >= :newOrder AND l.sortOrder < :oldOrder")
    void shiftSortOrderRangeUp(@Param("sectionId") String sectionId, @Param("newOrder") Integer newOrder, @Param("oldOrder") Integer oldOrder);

    @Query("SELECT l FROM Lesson l WHERE " +
           "(:sectionId IS NULL OR l.section.id = :sectionId) " +
           "AND (:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:lessonType IS NULL OR l.lessonType = :lessonType) " +
           "AND (:status IS NULL OR l.status = :status)")
    Page<Lesson> searchLessons(
            @Param("sectionId") String sectionId,
            @Param("keyword") String keyword,
            @Param("lessonType") Lesson.LessonType lessonType,
            @Param("status") Lesson.LessonStatus status,
            Pageable pageable
    );
}
