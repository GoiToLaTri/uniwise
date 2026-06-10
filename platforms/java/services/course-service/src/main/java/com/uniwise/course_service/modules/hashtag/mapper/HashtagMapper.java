package com.uniwise.course_service.modules.hashtag.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.HashtagCreateRequest;
import com.uniwise.common.dto.request.HashtagUpdateRequest;
import com.uniwise.common.dto.response.HashtagResponse;
import com.uniwise.course_service.modules.hashtag.entity.Hashtag;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface HashtagMapper {
 
    // ===== CREATE REQUEST → ENTITY =====
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "courseCount", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "courseHashtags", ignore = true)
    Hashtag toEntity(HashtagCreateRequest request);
 
    // ===== UPDATE REQUEST → ENTITY =====
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "courseCount", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    @Mapping(target = "courseHashtags", ignore = true)
    void updateEntity(HashtagUpdateRequest request, @MappingTarget Hashtag hashtag);
 
    // ===== ENTITY → RESPONSE =====
    // courseHashtags không expose ra ngoài
    @Mapping(target = "courseCount", source = "courseCount")
    HashtagResponse toResponse(Hashtag hashtag);
}
