package com.uniwise.identity_service.modules.account.impl;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.dto.response.PermissionResponse;
import com.uniwise.common.dto.response.RoleResponse;
import com.uniwise.identity.account.v1.AssignRolesRequest;
import com.uniwise.identity.account.v1.AssignRolesResponse;
import com.uniwise.identity.account.v1.RevokeRolesRequest;
import com.uniwise.identity.account.v1.RevokeRolesResponse;
import com.uniwise.identity_service.modules.account.AccountGrpcService;
import com.uniwise.identity_service.modules.account.AccountService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountGrpcServiceImpl implements AccountGrpcService {
    AccountService accountService;

    @Override
    public AssignRolesResponse assignRoles(AssignRolesRequest request) {
        AccountResponse accountResponse = accountService.assignRoles(request.getAccountId(), Set.copyOf(request.getRoleNamesList()));
        return AssignRolesResponse.newBuilder()
                .setAccount(toProtoAccount(accountResponse))
                .build();
    }

    @Override
    public RevokeRolesResponse revokeRoles(RevokeRolesRequest request) {
        AccountResponse accountResponse = accountService.revokeRoles(request.getAccountId(), Set.copyOf(request.getRoleNamesList()));
        return RevokeRolesResponse.newBuilder()
                .setAccount(toProtoAccount(accountResponse))
                .build();
    }

    private com.uniwise.identity.account.v1.Account toProtoAccount(AccountResponse response) {
        com.uniwise.identity.account.v1.Account.Builder builder = com.uniwise.identity.account.v1.Account.newBuilder()
                .setId(response.getId() != null ? response.getId() : "")
                .setEmail(response.getEmail() != null ? response.getEmail() : "")
                .setProvider(response.getProvider() != null ? response.getProvider() : "");

        if (response.getRoles() != null) {
            response.getRoles().stream()
                    .map(this::toProtoRole)
                    .forEach(builder::addRoles);
        }

        return builder.build();
    }

    private com.uniwise.identity.account.v1.Role toProtoRole(RoleResponse roleResponse) {
        com.uniwise.identity.account.v1.Role.Builder builder = com.uniwise.identity.account.v1.Role.newBuilder()
                .setId(roleResponse.getId() != null ? roleResponse.getId() : 0L)
                .setDisplayName(roleResponse.getDisplayName() != null ? roleResponse.getDisplayName() : "")
                .setName(roleResponse.getName() != null ? roleResponse.getName() : "")
                .setDescription(roleResponse.getDescription() != null ? roleResponse.getDescription() : "")
                .setIsActive(Boolean.TRUE.equals(roleResponse.getIsActive()));

        if (roleResponse.getPermissions() != null) {
            roleResponse.getPermissions().stream()
                    .map(this::toProtoPermission)
                    .forEach(builder::addPermissions);
        }

        return builder.build();
    }

    private com.uniwise.identity.account.v1.Permission toProtoPermission(PermissionResponse permissionResponse) {
        return com.uniwise.identity.account.v1.Permission.newBuilder()
                .setId(permissionResponse.getId() != null ? permissionResponse.getId() : 0L)
                .setName(permissionResponse.getName() != null ? permissionResponse.getName() : "")
                .setDescription(permissionResponse.getDescription() != null ? permissionResponse.getDescription() : "")
                .build();
    }
}
