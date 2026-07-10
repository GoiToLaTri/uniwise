package com.uniwise.course_service.modules.course_mgmt.lesson.helper;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.uniwise.course_service.modules.course_mgmt.lesson.LessonService;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonServiceHelper {
    ApplicationContext applicationContext;

    public Lesson getLessonEntityById(String id) {
        return applicationContext.getBean(LessonService.class).getEntityById(id);
    }

    public long countLessonsByCourseId(String courseId) {
        return applicationContext.getBean(LessonService.class).countByCourseId(courseId);
    }
}
