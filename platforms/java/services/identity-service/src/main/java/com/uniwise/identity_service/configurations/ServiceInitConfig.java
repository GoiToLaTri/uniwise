package com.uniwise.identity_service.configurations;

import java.util.Set;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.RoleCreateRequest;
import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.RoleError;
import com.uniwise.identity_service.modules.account.AccountService;
import com.uniwise.identity_service.modules.role.RoleService;

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
    RoleCreateRequest[] defaultRoles = {
            RoleCreateRequest.builder().name("USER").description("Default role for all users").build(),
            RoleCreateRequest.builder().name("ADMIN").description("Role with full permissions").build(),
    };
    AccountCreateRequest adminAccountRequest = AccountCreateRequest.builder()
            .name("Admin")
            .email("admin@uniwise.com")
            .password("00000000")
            .build();

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource", value = "driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
    public ApplicationRunner init() {
        log.info("Starting Identity Service initialization...");
        return args -> {
            initializeRoles();
            initializeAdminAccount();
            log.warn(
                    "Initialization complete. Please change the default admin password immediately after first login.");
            log.info("Identity Service initialized successfully");
        };
    }

    private void initializeRoles() {
        for (RoleCreateRequest roleRequest : defaultRoles) {
            try {
                roleService.create(roleRequest);
                log.info("Default role '{}' created successfully", roleRequest.getName());
            } catch (HttpException e) {
                if (e.getError().getCode().equals(RoleError.ROLE_ALREADY_EXISTS.getCode()))
                    log.warn("Default role '{}' already exists", roleRequest.getName());
                return;
            } catch (Exception e) {
                log.error("Failed to create default role '{}': {}", roleRequest.getName(), e.getMessage());
            }
        }
    }

    private void initializeAdminAccount() {
        try {
            AccountResponse adminAccount = accountService.create(adminAccountRequest);
            Set<String> roles = Set.of(defaultRoles[1].getName());
            accountService.assignRoles(adminAccount.getId(), roles);
            log.info("Admin account created successfully with email: {}", adminAccountRequest.getEmail());
        } catch (HttpException e) {
            log.warn("Admin account already exists");
        } catch (Exception e) {
            log.error("Failed to create admin account: {}", e.getMessage());
        }
    }
}