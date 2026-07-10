package com.uniwise.course_service.modules.learning_progress.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uniwise.common.dto.response.UserCourseDto;
import com.uniwise.common.dto.response.UserLessonDto;
import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;

@Mapper(componentModel = "spring")
public interface LearningProgressMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "publicId", source = "course.publicId")
    @Mapping(target = "title", source = "course.title")
    @Mapping(target = "thumbnail", source = "course.thumbnailUrl")
    @Mapping(target = "progressPercentage", ignore = true) // Will be calculated dynamically
    UserCourseDto toUserCourseDto(UserCourse userCourse);

    @Mapping(target = "lessonId", source = "lesson.id")
    UserLessonDto toUserLessonDto(UserLesson userLesson);
}
