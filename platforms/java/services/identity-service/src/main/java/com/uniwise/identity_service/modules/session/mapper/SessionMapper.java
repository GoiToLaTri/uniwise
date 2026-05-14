package com.uniwise.identity_service.modules.session.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.identity_service.modules.session.entity.Session;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SessionMapper {
    SessionResponse toResponse(Session session);
}
