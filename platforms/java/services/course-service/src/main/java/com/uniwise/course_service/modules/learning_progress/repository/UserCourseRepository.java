package com.uniwise.course_service.modules.learning_progress.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;

public interface UserCourseRepository extends JpaRepository<UserCourse, UserCourse.UserCourseId> {
    @Query("SELECT COUNT(uc) > 0 FROM UserCourse uc " +
           "JOIN uc.course c " +
           "WHERE uc.accountId = :accountId AND c.id = :courseId")
    boolean existsByAccountIdAndCourseId(@Param("accountId") String accountId, @Param("courseId") String courseId);

    @Query("SELECT uc FROM UserCourse uc WHERE uc.accountId = :accountId AND uc.course.id = :courseId")
    java.util.Optional<UserCourse> findByAccountIdAndCourseId(@Param("accountId") String accountId, @Param("courseId") String courseId);
}
