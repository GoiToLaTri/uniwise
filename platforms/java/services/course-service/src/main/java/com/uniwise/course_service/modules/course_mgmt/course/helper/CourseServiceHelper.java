package com.uniwise.course_service.modules.course_mgmt.course.helper;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.uniwise.course_service.modules.course_mgmt.course.CourseService;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceHelper {
    ApplicationContext applicationContext;

    public Course getCourseEntityById(String id) {
        return applicationContext.getBean(CourseService.class).getEntityById(id);
    }

    public void incrementStudentCountAndQueueSync(String courseId) {
        applicationContext.getBean(CourseService.class).incrementStudentCountAndQueueSync(courseId);
    }

    public void incrementTotalSectionsAndQueueSync(String courseId) {
        applicationContext.getBean(CourseService.class).incrementTotalSectionsAndQueueSync(courseId);
    }

    public void decrementTotalSectionsAndQueueSync(String courseId) {
        applicationContext.getBean(CourseService.class).decrementTotalSectionsAndQueueSync(courseId);
    }

    public void incrementTotalLessonsAndQueueSync(String courseId) {
        applicationContext.getBean(CourseService.class).incrementTotalLessonsAndQueueSync(courseId);
    }

    public void decrementTotalLessonsAndQueueSync(String courseId) {
        applicationContext.getBean(CourseService.class).decrementTotalLessonsAndQueueSync(courseId);
    }
}
