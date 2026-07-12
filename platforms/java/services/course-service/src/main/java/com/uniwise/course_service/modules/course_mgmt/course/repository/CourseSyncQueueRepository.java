package com.uniwise.course_service.modules.course_mgmt.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.course_mgmt.course.entity.CourseSyncQueue;
import jakarta.transaction.Transactional;

public interface CourseSyncQueueRepository extends JpaRepository<CourseSyncQueue, String> {

    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO course_sync_queue (course_id, created_at) VALUES (:courseId, NOW())", nativeQuery = true)
    void insertIgnoreCourseId(@Param("courseId") String courseId);
}
