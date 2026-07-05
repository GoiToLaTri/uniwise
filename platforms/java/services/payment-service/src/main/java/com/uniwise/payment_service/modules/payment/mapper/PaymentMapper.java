package com.uniwise.payment_service.modules.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.response.PaymentResponse;
import com.uniwise.payment_service.modules.payment.entity.Payment;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {
    PaymentResponse toResponse(Payment payment);
}
