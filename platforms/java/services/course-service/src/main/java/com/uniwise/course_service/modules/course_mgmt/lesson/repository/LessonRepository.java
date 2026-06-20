package com.uniwise.course_service.modules.course_mgmt.lesson.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, String> {

    boolean existsBySectionIdAndSortOrder(String sectionId, Integer sortOrder);

    boolean existsBySectionIdAndSortOrderAndPublicIdNot(String sectionId, Integer sortOrder, String publicId);

    java.util.Optional<Lesson> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

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
