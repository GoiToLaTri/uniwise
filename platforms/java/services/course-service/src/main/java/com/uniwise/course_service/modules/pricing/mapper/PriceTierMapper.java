package com.uniwise.course_service.modules.pricing.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.CreatePriceTierRequest;
import com.uniwise.common.dto.request.UpdatePriceTierRequest;
import com.uniwise.common.dto.response.PriceTierResponse;
import com.uniwise.course_service.modules.pricing.entity.PriceTier;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PriceTierMapper {

    /**
     * Maps a creation request to a new {@link PriceTier} entity.
     * Auto-generated fields ({@code id}, {@code createdAt}, {@code updatedAt})
     * are explicitly ignored – they are set by the entity lifecycle / UUID generator.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courses", ignore = true)
    PriceTier toEntity(CreatePriceTierRequest request);

    /**
     * Partially updates an existing {@link PriceTier} entity from an update request.
     * {@code null} fields in the request are skipped (patch semantics).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "courses", ignore = true)
    void updateEntity(UpdatePriceTierRequest request, @MappingTarget PriceTier target);

    /**
     * Maps a {@link PriceTier} entity to its response DTO.
     * {@code courseCount} is derived separately in the service layer.
     */
    @Mapping(target = "courseCount", ignore = true)
    PriceTierResponse toResponse(PriceTier priceTier);
}