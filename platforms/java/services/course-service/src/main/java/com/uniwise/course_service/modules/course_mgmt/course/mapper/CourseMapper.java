package com.uniwise.course_service.modules.course_mgmt.course.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.CourseCreateRequest;
import com.uniwise.common.dto.request.CourseUpdateRequest;
import com.uniwise.common.dto.response.CourseResponse;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.section.mapper.SectionMapper;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {SectionMapper.class}
)
public interface CourseMapper {

    // ===== CREATE REQUEST → ENTITY =====
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "priceTier", ignore = true) // Set manually in Service
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "courseHashtags", ignore = true)
    @Mapping(target = "userCourses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Course toEntity(CourseCreateRequest request);

    // ===== UPDATE REQUEST → ENTITY =====
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "priceTier", ignore = true)
    @Mapping(target = "sections", ignore = true)
    @Mapping(target = "courseHashtags", ignore = true)
    @Mapping(target = "userCourses", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntity(CourseUpdateRequest request, @MappingTarget Course course);

    // ===== ENTITY → RESPONSE =====
    @Mapping(target = "priceTierId", source = "priceTier.id")
    CourseResponse toResponse(Course course);
}
