package com.uniwise.course_service.modules.learning_progress;

import java.util.List;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.UserCourseDto;
import com.uniwise.common.dto.response.CourseProgressResponse;
import com.uniwise.common.dto.request.SyncVideoPositionRequest;

public interface LearningProgressService {
    boolean isEnrolled(String accountId, String courseId);
    List<UserLesson> getUserLessonsProgress(String accountId, String courseId);
    void enrollUser(String accountId, String courseId, boolean isPaid);
    
    PageResponse<UserCourseDto> getMyEnrolledCourses(String accountId, int page, int size);
    void enrollFreeCourse(String accountId, String courseId);
    CourseProgressResponse getCourseProgress(String accountId, String courseId);
    void syncVideoPosition(String accountId, String lessonId, SyncVideoPositionRequest request);
    void markLessonAsCompleted(String accountId, String lessonId);
}
