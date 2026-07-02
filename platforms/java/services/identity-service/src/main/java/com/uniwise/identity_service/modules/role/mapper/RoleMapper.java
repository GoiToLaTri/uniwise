package com.uniwise.identity_service.modules.role.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.RoleCreateRequest;
import com.uniwise.common.dto.request.RoleUpdateRequest;
import com.uniwise.common.dto.response.RoleAdminResponse;
import com.uniwise.common.dto.response.RoleResponse;
import com.uniwise.identity_service.modules.role.entity.Role;

import org.mapstruct.InjectionStrategy;
import com.uniwise.identity_service.modules.permission.mapper.PermissionMapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {
        PermissionMapper.class }, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    void updateEntity(RoleUpdateRequest request, @MappingTarget Role role);

    RoleResponse toResponse(Role role);

    RoleAdminResponse toAdminResponse(Role role);
}
