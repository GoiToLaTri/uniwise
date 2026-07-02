package com.uniwise.course_service.modules.course_mgmt.section.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.SectionCreateRequest;
import com.uniwise.common.dto.request.SectionUpdateRequest;
import com.uniwise.common.dto.response.SectionResponse;
import com.uniwise.course_service.modules.course_mgmt.lesson.mapper.LessonMapper;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {
                LessonMapper.class })
public interface SectionMapper {

        // ===== CREATE REQUEST → ENTITY =====
        @Mapping(target = "id", ignore = true)
        @Mapping(target = "publicId", ignore = true)
        @Mapping(target = "course", ignore = true) // Set manually in Service
        @Mapping(target = "lessons", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        Section toEntity(SectionCreateRequest request);

        // ===== UPDATE REQUEST → ENTITY =====
        @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
        @Mapping(target = "id", ignore = true)
        @Mapping(target = "publicId", ignore = true)
        @Mapping(target = "course", ignore = true)
        @Mapping(target = "lessons", ignore = true)
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        void updateEntity(SectionUpdateRequest request, @MappingTarget Section section);

        // ===== ENTITY → RESPONSE =====
        @Mapping(target = "courseId", source = "course.id")
        SectionResponse toResponse(Section section);
}
