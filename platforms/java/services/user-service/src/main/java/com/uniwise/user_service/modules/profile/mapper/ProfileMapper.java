package com.uniwise.user_service.modules.profile.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.ProfileCreateRequest;
import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.user_service.modules.profile.entity.Profile;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountId", ignore = true)
    Profile toEntity(ProfileCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "accountId", ignore = true)
    void updateEntity(ProfileUpdateRequest request, @MappingTarget Profile profile);

    ProfileResponse toResponse(Profile profile);
}
