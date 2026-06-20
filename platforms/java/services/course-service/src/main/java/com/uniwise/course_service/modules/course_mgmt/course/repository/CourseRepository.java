package com.uniwise.course_service.modules.course_mgmt.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;

public interface CourseRepository extends JpaRepository<Course, String> {
}
