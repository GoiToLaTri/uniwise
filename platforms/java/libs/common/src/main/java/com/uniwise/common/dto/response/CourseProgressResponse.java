package com.uniwise.common.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseProgressResponse {
    String courseId;
    Instant enrolledAt;
    Double progressPercentage;
    Integer completedLessonsCount;
    Integer totalLessonsCount;
    List<UserLessonDto> userLessons;
}
