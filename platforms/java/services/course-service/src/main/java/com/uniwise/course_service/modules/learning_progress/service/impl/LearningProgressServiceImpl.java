package com.uniwise.course_service.modules.learning_progress.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;
import com.uniwise.course_service.modules.learning_progress.repository.UserCourseRepository;
import com.uniwise.course_service.modules.learning_progress.repository.UserLessonRepository;
import com.uniwise.course_service.modules.course_mgmt.course.helper.CourseServiceHelper;
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
    CourseServiceHelper courseServiceHelper;

    @Override
    @Transactional(readOnly = true)
    public boolean isEnrolled(String accountId, String courseId) {
        log.info("Checking enrollment status for accountId: {} and courseId: {}", accountId, courseId);
        if (accountId == null || accountId.isBlank() || courseId == null || courseId.isBlank()) {
            return false;
        }
        return userCourseRepository.existsByAccountIdAndCourseId(accountId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLesson> getUserLessonsProgress(String accountId, String courseId) {
        log.info("Fetching user lesson progress for accountId: {} and courseId: {}", accountId, courseId);
        if (accountId == null || accountId.isBlank() || courseId == null || courseId.isBlank()) {
            return List.of();
        }
        return userLessonRepository.findByAccountIdAndCourseId(accountId, courseId);
    }

    @Override
    @Transactional
    public void enrollUser(String accountId, String courseId, boolean isPaid) {
        log.info("Enrolling accountId: {} to courseId: {}, isPaid: {}", accountId, courseId, isPaid);

        // 1. Check if enrollment already exists
        var existingOpt = userCourseRepository.findByAccountIdAndCourseId(accountId, courseId);
        if (existingOpt.isPresent()) {
            UserCourse existing = existingOpt.get();
            if (isPaid && !Boolean.TRUE.equals(existing.getIsPaid())) {
                existing.setIsPaid(true);
                userCourseRepository.save(existing);
                log.info("Updated existing enrollment to PAID for accountId: {}, courseId: {}", accountId, courseId);
            }
            return;
        }

        // 2. Fetch Course via Helper (resolves circular dependency)
        com.uniwise.course_service.modules.course_mgmt.course.entity.Course course = courseServiceHelper.getCourseEntityById(courseId);

        // 3. Create new enrollment
        UserCourse userCourse = UserCourse.builder()
                .accountId(accountId)
                .course(course)
                .isPaid(isPaid)
                .build();

        userCourseRepository.save(userCourse);
        log.info("Successfully enrolled accountId: {} to courseId: {}", accountId, courseId);
    }
}
