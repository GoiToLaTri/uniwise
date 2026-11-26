package com.uniwise.identity_service.modules.account.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AccountError;
import com.uniwise.common.exception.errors.RoleError;
import com.uniwise.identity_service.modules.account.AccountRoleManager;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.account.mapper.AccountMapper;
import com.uniwise.identity_service.modules.account.repository.AccountRepository;
import com.uniwise.identity_service.modules.redis.RedisService;
import com.uniwise.identity_service.modules.role.RoleService;
import com.uniwise.identity_service.modules.role.entity.Role;
import com.uniwise.identity_service.modules.session.SessionService;
import com.uniwise.identity_service.modules.session.entity.Session;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountRoleManagerImpl implements AccountRoleManager {
    AccountRepository accountRepository;
    AccountMapper accountMapper;
    RoleService roleService;
    SessionService sessionService;
    RedisService redisService;

    @Override
    @Transactional
    public AccountResponse assignRoles(String accountId, Set<String> roleNames) {
        Account account = getAccountForUpdate(accountId);
        if (roleNames == null || roleNames.isEmpty())
            return accountMapper.toResponse(account);

        Set<Role> requestedRoles = getRequiredRoles(roleNames);
        Set<String> currentRoleNames = account.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        List<Role> rolesToAdd = requestedRoles.stream()
                .filter(role -> !currentRoleNames.contains(role.getName()))
                .toList();

        if (rolesToAdd.isEmpty())
            return accountMapper.toResponse(account);

        rolesToAdd.forEach(role -> role.setUserCount(currentUserCount(role) + 1));
        account.getRoles().addAll(rolesToAdd);
        Account updated = accountRepository.save(account);
        invalidateAccessTokens(accountId);
        log.info("Roles {} assigned to account with id: {}",
                rolesToAdd.stream().map(Role::getName).toList(), accountId);
        return accountMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public AccountResponse revokeRoles(String accountId, Set<String> roleNames) {
        Account account = getAccountForUpdate(accountId);
        if (roleNames == null || roleNames.isEmpty())
            return accountMapper.toResponse(account);

        getRequiredRoles(roleNames);
        List<Role> rolesToRemove = account.getRoles().stream()
                .filter(role -> roleNames.contains(role.getName()))
                .toList();

        if (rolesToRemove.isEmpty())
            return accountMapper.toResponse(account);

        account.getRoles().removeIf(role -> roleNames.contains(role.getName()));
        rolesToRemove.forEach(role ->
                role.setUserCount(Math.max(0, currentUserCount(role) - 1)));
        Account updated = accountRepository.save(account);
        invalidateAccessTokens(accountId);
        log.info("Roles {} revoked from account with id: {}",
                rolesToRemove.stream().map(Role::getName).toList(), accountId);
        return accountMapper.toResponse(updated);
    }

    private void invalidateAccessTokens(String accountId) {
        List<String> accessKeys = sessionService.findActiveByAccountId(accountId).stream()
                .map(Session::getToken)
                .filter(token -> token != null && !token.isBlank())
                .map(token -> "access:" + token)
                .toList();
        if (accessKeys.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisService.deleteMultipleKey(accessKeys);
                }
            });
        } else {
            redisService.deleteMultipleKey(accessKeys);
        }
    }

    private Account getAccountForUpdate(String accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
    }

    private Set<Role> getRequiredRoles(Set<String> roleNames) {
        Set<Role> roles = roleService.getByNames(roleNames);
        if (roles.size() != roleNames.size())
            throw new HttpException(RoleError.ROLE_NOT_FOUND);
        return roles;
    }

    private int currentUserCount(Role role) {
        return role.getUserCount() == null ? 0 : role.getUserCount();
    }
}
