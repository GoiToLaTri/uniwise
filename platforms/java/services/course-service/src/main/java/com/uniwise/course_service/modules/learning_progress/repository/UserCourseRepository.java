package com.uniwise.course_service.modules.learning_progress.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;

public interface UserCourseRepository extends JpaRepository<UserCourse, UserCourse.UserCourseId> {
    @Query("SELECT COUNT(uc) > 0 FROM UserCourse uc " +
           "JOIN uc.course c " +
           "WHERE uc.userId = :userId AND c.id = :courseId")
    boolean existsByUserIdAndCourseId(@Param("userId") String userId, @Param("courseId") String courseId);
}
