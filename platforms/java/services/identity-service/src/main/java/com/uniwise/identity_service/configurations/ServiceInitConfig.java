package com.uniwise.identity_service.configurations;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.PermissionCreateRequest;
import com.uniwise.common.dto.request.RoleCreateRequest;
import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AccountError;
import com.uniwise.common.exception.errors.PermissionError;
import com.uniwise.common.exception.errors.RoleError;
import com.uniwise.identity_service.modules.account.AccountRoleManager;
import com.uniwise.identity_service.modules.account.AccountService;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.permission.PermissionService;
import com.uniwise.identity_service.modules.role.RoleService;
import com.uniwise.identity_service.modules.role.entity.Role;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ServiceInitConfig {
    RoleService roleService;
    AccountService accountService;
    AccountRoleManager accountRoleManager;
    PermissionService permissionService;
    PasswordEncoder passwordEncoder;

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource", value = "driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
    public ApplicationRunner init(
            Environment environment,
            @Value("${ADMIN_EMAIL:admin@uniwise.com}") String adminEmail,
            @Value("${ADMIN_PASSWORD:00000000}") String adminPassword) {
        log.info("Starting Identity Service initialization...");
        return args -> {
            initializePermissions();
            initializeRoles();
            initializeRolePermissions();
            if (environment.acceptsProfiles(Profiles.of("dev", "docker"))) {
                initializeAdminAccount(IdentitySeedData.adminAccount(adminEmail, adminPassword));
                warnIfAdminUsesDefaultPassword(adminEmail);
            }
            log.info("Identity Service initialized successfully");
        };
    }

    private void initializePermissions() {
        for (PermissionCreateRequest permissionRequest : IdentitySeedData.DEFAULT_PERMISSIONS) {
            try {
                permissionService.create(permissionRequest);
                log.info("Default permission '{}' created successfully", permissionRequest.getName());
            } catch (HttpException e) {
                if (PermissionError.PERMISSION_ALREADY_EXISTS.getCode().equals(e.getError().getCode())) {
                    log.debug("Default permission '{}' already exists", permissionRequest.getName());
                } else {
                    log.error("Failed to create default permission '{}': {}",
                            permissionRequest.getName(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to create default permission '{}': {}",
                        permissionRequest.getName(), e.getMessage());
            }
        }
    }

    private void initializeRoles() {
        for (RoleCreateRequest roleRequest : IdentitySeedData.DEFAULT_ROLES) {
            try {
                roleService.create(roleRequest);
                log.info("Default role '{}' created successfully", roleRequest.getName());
            } catch (HttpException e) {
                if (RoleError.ROLE_ALREADY_EXISTS.getCode().equals(e.getError().getCode())) {
                    log.debug("Default role '{}' already exists", roleRequest.getName());
                } else {
                    log.error("Failed to create default role '{}': {}",
                            roleRequest.getName(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed to create default role '{}': {}", roleRequest.getName(), e.getMessage());
            }
        }
    }

    private void initializeRolePermissions() {
        Set<String> roleNames = IdentitySeedData.DEFAULT_ROLE_PERMISSIONS.keySet();
        var rolesByName = roleService.getByNames(roleNames).stream()
                .collect(Collectors.toMap(Role::getName, Function.identity()));

        IdentitySeedData.DEFAULT_ROLE_PERMISSIONS.forEach((roleName, permissionNames) -> {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                log.error("Cannot assign default permissions because role '{}' was not found", roleName);
                return;
            }

            try {
                roleService.ensurePermissions(role.getId(), permissionNames);
                log.info("Default permissions ensured for role '{}'", roleName);
            } catch (Exception e) {
                log.error("Failed to assign default permissions to role '{}': {}", roleName, e.getMessage());
            }
        });
    }

    private void initializeAdminAccount(AccountCreateRequest adminAccountRequest) {
        String adminAccountId;
        try {
            AccountResponse adminAccount = accountService.create(adminAccountRequest);
            adminAccountId = adminAccount.getId();
            log.info("Admin account created successfully with email: {}",
                    adminAccountRequest.getEmail());
        } catch (HttpException e) {
            if (!AccountError.ACCOUNT_ALREADY_EXISTS.getCode().equals(e.getError().getCode())) {
                log.error("Failed to create admin account: {}", e.getError().getMessage());
                return;
            }
            try {
                adminAccountId = accountService.getByEmail(
                        adminAccountRequest.getEmail()).getId();
                log.debug("Admin account already exists");
            } catch (Exception lookupException) {
                log.error("Failed to load existing admin account: {}", lookupException.getMessage());
                return;
            }
        } catch (Exception e) {
            log.error("Failed to create admin account: {}", e.getMessage());
            return;
        }

        try {
            accountRoleManager.assignRoles(
                    adminAccountId, Set.of(IdentitySeedData.ADMIN_ROLE_NAME));
            log.info("Administrator role ensured for account '{}'",
                    adminAccountRequest.getEmail());
        } catch (Exception e) {
            log.error("Failed to ensure administrator role: {}", e.getMessage());
        }
    }

    private void warnIfAdminUsesDefaultPassword(String adminEmail) {
        try {
            Account adminAccount = accountService.getByEmail(adminEmail);
            boolean usesDefaultPassword = passwordEncoder.matches(
                    IdentitySeedData.DEFAULT_ADMIN_PASSWORD,
                    adminAccount.getPassword());

            if (usesDefaultPassword) {
                log.warn(
                        "SECURITY WARNING: Administrator account '{}' is still using the default initialization password. Change it immediately.",
                        adminEmail);
            }
        } catch (Exception e) {
            log.error("Unable to verify whether the administrator account uses the default password: {}",
                    e.getMessage());
        }
    }
}
