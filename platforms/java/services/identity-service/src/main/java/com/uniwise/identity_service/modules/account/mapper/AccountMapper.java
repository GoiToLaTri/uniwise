package com.uniwise.identity_service.modules.account.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.AccountUpdateRequest;
import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.role.entity.Role;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    Account toEntity(AccountCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateEntity(AccountUpdateRequest request, @MappingTarget Account account);

    AccountResponse toResponse(Account account);

    // Helper: map Set<Role> → Set<String> for role IDs if needed
    default Set<Long> mapRolesToIds(Set<Role> roles) {
        if (roles == null)
            return null;
        return roles.stream().map(Role::getId).collect(Collectors.toSet());
    }
}
