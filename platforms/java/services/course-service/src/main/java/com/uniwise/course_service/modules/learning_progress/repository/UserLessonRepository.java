package com.uniwise.course_service.modules.learning_progress.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;
import java.util.List;

public interface UserLessonRepository extends JpaRepository<UserLesson, UserLesson.UserLessonId> {
    @Query("SELECT ul FROM UserLesson ul " +
           "JOIN ul.lesson l " +
           "JOIN l.section s " +
           "JOIN s.course c " +
           "WHERE ul.accountId = :accountId AND c.id = :courseId")
    List<UserLesson> findByAccountIdAndCourseId(@Param("accountId") String accountId, @Param("courseId") String courseId);
}
