package com.uniwise.course_service.modules.learning_progress.service;

import java.util.List;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;

public interface LearningProgressService {
    boolean isEnrolled(String userId, String courseId);
    List<UserLesson> getUserLessonsProgress(String userId, String courseId);
}
