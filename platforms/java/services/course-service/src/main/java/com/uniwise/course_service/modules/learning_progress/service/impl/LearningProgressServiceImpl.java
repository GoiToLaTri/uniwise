package com.uniwise.course_service.modules.learning_progress.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;
import com.uniwise.course_service.modules.learning_progress.repository.UserCourseRepository;
import com.uniwise.course_service.modules.learning_progress.repository.UserLessonRepository;
import com.uniwise.course_service.modules.learning_progress.service.LearningProgressService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LearningProgressServiceImpl implements LearningProgressService {

    UserCourseRepository userCourseRepository;
    UserLessonRepository userLessonRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(String userId, String courseId) {
        log.info("Checking enrollment status for userId: {} and courseId: {}", userId, courseId);
        if (userId == null || userId.isBlank() || courseId == null || courseId.isBlank()) {
            return false;
        }
        return userCourseRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLesson> getUserLessonsProgress(String userId, String courseId) {
        log.info("Fetching user lesson progress for userId: {} and courseId: {}", userId, courseId);
        if (userId == null || userId.isBlank() || courseId == null || courseId.isBlank()) {
            return List.of();
        }
        return userLessonRepository.findByUserIdAndCourseId(userId, courseId);
    }
}
