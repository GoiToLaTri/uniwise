package com.uniwise.course_service.modules.course_mgmt.lesson.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.LessonCreateRequest;
import com.uniwise.common.dto.request.LessonUpdateRequest;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LessonMapper {

    // ===== CREATE REQUEST → ENTITY =====
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "section", ignore = true) // Set manually in Service
    @Mapping(target = "status", ignore = true) // Defaults to PROCESSING
    @Mapping(target = "userLessons", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lessonType", expression = "java(mapLessonType(request.getLessonType()))")
    Lesson toEntity(LessonCreateRequest request);

    // ===== UPDATE REQUEST → ENTITY =====
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "section", ignore = true)
    @Mapping(target = "userLessons", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lessonType", expression = "java(request.getLessonType() == null ? null : mapLessonType(request.getLessonType()))")
    @Mapping(target = "status", expression = "java(request.getStatus() == null ? null : mapLessonStatus(request.getStatus()))")
    void updateEntity(LessonUpdateRequest request, @MappingTarget Lesson lesson);

    // ===== ENTITY → RESPONSE =====
    @Mapping(target = "sectionId", source = "section.id")
    LessonResponse toResponse(Lesson lesson);

    // Helper methods for enum mapping
    default Lesson.LessonType mapLessonType(String value) {
        try {
            return Lesson.LessonType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new com.uniwise.common.exception.HttpException(
                com.uniwise.common.exception.errors.LessonError.LESSON_TYPE_INVALID
            );
        }
    }

    default Lesson.LessonStatus mapLessonStatus(String value) {
        try {
            return Lesson.LessonStatus.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new com.uniwise.common.exception.HttpException(
                com.uniwise.common.exception.errors.LessonError.LESSON_STATUS_INVALID
            );
        }
    }
}
