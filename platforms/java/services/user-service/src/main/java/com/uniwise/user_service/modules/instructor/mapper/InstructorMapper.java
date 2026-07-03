package com.uniwise.user_service.modules.instructor.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.InstructorProfileCreateRequest;
import com.uniwise.common.dto.request.InstructorProfileUpdateRequest;
import com.uniwise.common.dto.response.DegreeDto;
import com.uniwise.common.dto.response.ExpertiseDto;
import com.uniwise.common.dto.response.InstructorProfileResponse;
import com.uniwise.user_service.modules.instructor.entity.DegreeCertificate;
import com.uniwise.user_service.modules.instructor.entity.Expertise;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InstructorMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reviewComment", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InstructorProfile toEntity(InstructorProfileCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reviewComment", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "degrees", ignore = true)
    @Mapping(target = "expertises", ignore = true)
    void updateEntity(InstructorProfileUpdateRequest request, @MappingTarget InstructorProfile profile);

    @Mapping(target = "accountId", source = "profile.accountId")
    InstructorProfileResponse toResponse(InstructorProfile profile);

    DegreeCertificate toDegreeEntity(DegreeDto dto);

    Expertise toExpertiseEntity(ExpertiseDto dto);

    DegreeDto toDegreeDto(DegreeCertificate degree);

    ExpertiseDto toExpertiseDto(Expertise expertise);

    List<DegreeDto> toDegreeDtos(Set<DegreeCertificate> entities);

    List<ExpertiseDto> toExpertiseDtos(Set<Expertise> entities);

    default String toString(LocalDateTime value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    default LocalDateTime toLocalDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
    }

    default String toString(LocalDate value) {
        return value == null ? null : value.format(DateTimeFormatter.ISO_DATE);
    }

    default LocalDate toLocalDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
    }
}
