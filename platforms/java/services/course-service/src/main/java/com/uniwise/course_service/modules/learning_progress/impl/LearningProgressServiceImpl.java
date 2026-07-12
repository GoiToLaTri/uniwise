package com.uniwise.course_service.modules.learning_progress.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.common.dto.request.SyncVideoPositionRequest;
import com.uniwise.common.dto.response.CourseProgressResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.UserCourseDto;
import com.uniwise.common.dto.response.UserLessonDto;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.CourseError;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.helper.CourseServiceHelper;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.course_mgmt.lesson.helper.LessonServiceHelper;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;
import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;
import com.uniwise.course_service.modules.learning_progress.mapper.LearningProgressMapper;
import com.uniwise.course_service.modules.learning_progress.repository.UserCourseRepository;
import com.uniwise.course_service.modules.learning_progress.repository.UserLessonRepository;

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
    LessonServiceHelper lessonServiceHelper;
    CourseServiceHelper courseServiceHelper;
    LearningProgressMapper learningProgressMapper;

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

    /**
     * Thực hiện ghi danh người dùng vào một khóa học.
     * Hàm này cũng xử lý trường hợp nâng cấp: nếu người dùng đã ghi danh trước đó (ví dụ: học thử/miễn phí)
     * và bây giờ mua khóa học (isPaid = true), hệ thống sẽ cập nhật trạng thái ghi danh thành đã thanh toán.
     * 
     * @param accountId ID của người dùng
     * @param courseId ID của khóa học
     * @param isPaid Trạng thái thanh toán của lần ghi danh này
     */
    @Override
    @Transactional
    public void enrollUser(String accountId, String courseId, boolean isPaid) {
        log.info("Enrolling accountId: {} to courseId: {}, isPaid: {}", accountId, courseId, isPaid);

        var existingOpt = userCourseRepository.findByAccountIdAndCourseId(accountId, courseId);
        if (existingOpt.isPresent()) {
            UserCourse existing = existingOpt.get();
            // Nếu yêu cầu ghi danh mới là có trả phí (isPaid = true) 
            // nhưng trạng thái trong DB lại chưa phải là đã thanh toán
            if (isPaid && !Boolean.TRUE.equals(existing.getIsPaid())) {
                existing.setIsPaid(true); // Cập nhật lại thành đã thanh toán
                userCourseRepository.save(existing);
                log.info("Updated existing enrollment to PAID for accountId: {}, courseId: {}", accountId, courseId);
            }
            // Trả về luôn để không tạo thêm bản ghi UserCourse mới
            return;
        }

        // Trường hợp người dùng chưa từng ghi danh, tạo mới bản ghi UserCourse
        Course course = courseServiceHelper.getCourseEntityById(courseId);

        UserCourse userCourse = UserCourse.builder()
                .accountId(accountId)
                .course(course)
                .isPaid(isPaid)
                .build();

        userCourseRepository.save(userCourse);
        
        // Trigger course sync queue and increment student count via helper to avoid layer violation
        courseServiceHelper.incrementStudentCountAndQueueSync(courseId);

        log.info("Successfully enrolled accountId: {} to courseId: {}", accountId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserCourseDto> getMyEnrolledCourses(String accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<UserCourse> userCoursePage = userCourseRepository.findByAccountId(accountId, pageable);

        List<UserCourseDto> content = userCoursePage.stream().map(uc -> {
            UserCourseDto dto = learningProgressMapper.toUserCourseDto(uc);
            long totalLessons = lessonServiceHelper.countLessonsByCourseId(uc.getCourse().getId());
            long completedLessons = userLessonRepository.findByAccountIdAndCourseId(accountId, uc.getCourse().getId())
                    .stream().filter(UserLesson::getIsCompleted).count();
            double percentage = totalLessons == 0 ? 0 : (double) completedLessons / totalLessons * 100;
            dto.setProgressPercentage(Math.round(percentage * 100.0) / 100.0);
            return dto;
        }).collect(Collectors.toList());

        return PageResponse.<UserCourseDto>builder()
                .content(content)
                .pageNumber(userCoursePage.getNumber() + 1)
                .pageSize(userCoursePage.getSize())
                .totalElements(userCoursePage.getTotalElements())
                .totalPages(userCoursePage.getTotalPages())
                .last(userCoursePage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void enrollFreeCourse(String accountId, String courseId) {
        Course course = courseServiceHelper.getCourseEntityById(courseId);
        if (course.getPriceTier() != null && course.getPriceTier().getPriceAmount() != null
                && course.getPriceTier().getPriceAmount().longValue() > 0) {
            throw new HttpException(CourseError.COURSE_NOT_FREE);
        }
        enrollUser(accountId, courseId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(String accountId, String courseId) {
        var userCourseOpt = userCourseRepository.findByAccountIdAndCourseId(accountId, courseId);

        if (userCourseOpt.isEmpty()) {
            Course course = courseServiceHelper.getCourseEntityById(courseId);
            if ((course.getCreatorId() != null && course.getCreatorId().equals(accountId)) || hasAdminAuthority()) {
                return CourseProgressResponse.builder()
                        .courseId(courseId)
                        .enrolledAt(null)
                        .progressPercentage(0.0)
                        .completedLessonsCount(0)
                        .totalLessonsCount((int) lessonServiceHelper.countLessonsByCourseId(courseId))
                        .userLessons(List.of())
                        .build();
            }
            throw new HttpException(CourseError.USER_NOT_ENROLLED);
        }

        UserCourse userCourse = userCourseOpt.get();

        List<UserLesson> userLessons = userLessonRepository.findByAccountIdAndCourseId(accountId, courseId);
        long totalLessons = lessonServiceHelper.countLessonsByCourseId(courseId);
        long completedLessons = userLessons.stream().filter(UserLesson::getIsCompleted).count();
        double percentage = totalLessons == 0 ? 0 : (double) completedLessons / totalLessons * 100;

        List<UserLessonDto> userLessonDtos = userLessons.stream()
                .map(learningProgressMapper::toUserLessonDto)
                .collect(Collectors.toList());

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .enrolledAt(userCourse.getEnrolledAt())
                .progressPercentage(Math.round(percentage * 100.0) / 100.0)
                .completedLessonsCount((int) completedLessons)
                .totalLessonsCount((int) totalLessons)
                .userLessons(userLessonDtos)
                .build();
    }

    @Override
    @Transactional
    public void syncVideoPosition(String accountId, String lessonId, SyncVideoPositionRequest request) {
        Lesson lesson = lessonServiceHelper.getLessonEntityById(lessonId);

        String courseId = lesson.getSection().getCourse().getId();
        if (!isEnrolled(accountId, courseId)) {
            Course course = courseServiceHelper.getCourseEntityById(courseId);
            if ((course.getCreatorId() != null && course.getCreatorId().equals(accountId)) || hasAdminAuthority()) {
                return;
            }
            throw new HttpException(CourseError.USER_NOT_ENROLLED);
        }

        UserLesson.UserLessonId id = new UserLesson.UserLessonId(accountId, lessonId);
        UserLesson userLesson = userLessonRepository.findById(id).orElseGet(() -> UserLesson.builder()
                .accountId(accountId)
                .lesson(lesson)
                .isCompleted(false)
                .build());

        userLesson.setLastWatchedPosition(request.getLastWatchedPosition());
        userLessonRepository.save(userLesson);
    }

    @Override
    @Transactional
    public void markLessonAsCompleted(String accountId, String lessonId) {
        Lesson lesson = lessonServiceHelper.getLessonEntityById(lessonId);

        String courseId = lesson.getSection().getCourse().getId();
        if (!isEnrolled(accountId, courseId)) {
            Course course = courseServiceHelper.getCourseEntityById(courseId);
            if ((course.getCreatorId() != null && course.getCreatorId().equals(accountId)) || hasAdminAuthority()) {
                return;
            }
            throw new HttpException(CourseError.USER_NOT_ENROLLED);
        }

        UserLesson.UserLessonId id = new UserLesson.UserLessonId(accountId, lessonId);
        UserLesson userLesson = userLessonRepository.findById(id).orElseGet(() -> UserLesson.builder()
                .accountId(accountId)
                .lesson(lesson)
                .isCompleted(false) // initialize
                .build());

        userLesson.setIsCompleted(true);
        userLessonRepository.save(userLesson);
    }

    private boolean hasAdminAuthority() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null)
            return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("admin:all"));
    }
}
