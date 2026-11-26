package com.uniwise.identity_service.modules.account.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.identity_service.modules.account.AccountRoleManager;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.account.mapper.AccountMapper;
import com.uniwise.identity_service.modules.account.repository.AccountRepository;
import com.uniwise.identity_service.modules.redis.RedisService;
import com.uniwise.identity_service.modules.role.RoleService;
import com.uniwise.identity_service.modules.role.entity.Role;
import com.uniwise.identity_service.modules.session.SessionService;
import com.uniwise.identity_service.modules.session.entity.Session;

@ExtendWith(MockitoExtension.class)
class AccountRoleManagerImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private RoleService roleService;
    @Mock
    private SessionService sessionService;
    @Mock
    private RedisService redisService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AccountRoleManager accountRoleManager;

    @InjectMocks
    private AccountRoleManagerImpl roleManager;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokeRoles_invalidatesActiveAccessTokensInRedis() {
        Role instructor = Role.builder().id(2L).name("INSTRUCTOR").userCount(3).isActive(true).build();
        Account account = Account.builder()
                .id("acc-1")
                .roles(new HashSet<>(Set.of(instructor)))
                .build();

        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(account));
        when(roleService.getByNames(Set.of("INSTRUCTOR"))).thenReturn(Set.of(instructor));
        when(accountRepository.save(account)).thenReturn(account);
        when(sessionService.findActiveByAccountId("acc-1")).thenReturn(List.of(
                Session.builder().id("s1").token("hash-1").build(),
                Session.builder().id("s2").token("hash-2").build()));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().id("acc-1").build());

        roleManager.revokeRoles("acc-1", Set.of("INSTRUCTOR"));

        assertEquals(2, instructor.getUserCount());
        verify(redisService).deleteMultipleKey(List.of("access:hash-1", "access:hash-2"));
    }

    @Test
    void assignRoles_invalidatesActiveAccessTokensInRedis() {
        Role instructor = Role.builder().id(2L).name("INSTRUCTOR").userCount(1).isActive(true).build();
        Account account = Account.builder()
                .id("acc-1")
                .roles(new HashSet<>())
                .build();

        when(accountRepository.findByIdForUpdate("acc-1")).thenReturn(Optional.of(account));
        when(roleService.getByNames(Set.of("INSTRUCTOR"))).thenReturn(Set.of(instructor));
        when(accountRepository.save(account)).thenReturn(account);
        when(sessionService.findActiveByAccountId("acc-1")).thenReturn(List.of(
                Session.builder().id("s1").token("hash-1").build()));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().id("acc-1").build());

        roleManager.assignRoles("acc-1", Set.of("INSTRUCTOR"));

        assertEquals(2, instructor.getUserCount());
        verify(redisService).deleteMultipleKey(List.of("access:hash-1"));
    }

    @Test
    void accountService_blocksAdminSelfDeleteToggleAndRevokeAdmin() {
        AccountServiceImpl accountService = new AccountServiceImpl(
                accountRepository,
                accountMapper,
                passwordEncoder,
                accountRoleManager,
                sessionService,
                redisService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-1",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        HttpException deleteEx = assertThrows(HttpException.class, () -> accountService.delete("admin-1"));
        assertEquals(AuthError.ACCESS_DENIED, deleteEx.getError());

        HttpException toggleEx = assertThrows(HttpException.class, () -> accountService.toggleActive("admin-1"));
        assertEquals(AuthError.ACCESS_DENIED, toggleEx.getError());

        HttpException revokeEx = assertThrows(HttpException.class,
                () -> accountService.revokeRoles("admin-1", Set.of("ADMIN")));
        assertEquals(AuthError.ACCESS_DENIED, revokeEx.getError());

        verifyNoInteractions(accountRepository, sessionService, redisService, accountRoleManager);
    }

    @Test
    void accountService_deleteCleansUpSessionsRedisAndRoleUserCount() {
        AccountServiceImpl accountService = new AccountServiceImpl(
                accountRepository,
                accountMapper,
                passwordEncoder,
                accountRoleManager,
                sessionService,
                redisService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-1",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        Role userRole = Role.builder().id(1L).name("USER").userCount(5).isActive(true).build();
        Account target = Account.builder()
                .id("user-2")
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
        Session session = Session.builder().id("sess-9").token("tok-9").build();

        when(accountRepository.findByIdForUpdate("user-2")).thenReturn(Optional.of(target));
        when(sessionService.deleteAllByAccountId("user-2")).thenReturn(List.of(session));

        accountService.delete("user-2");

        assertEquals(4, userRole.getUserCount());
        verify(sessionService).deleteAllByAccountId("user-2");
        verify(redisService).deleteMultipleKey(List.of("access:tok-9", "session:sess-9"));
        verify(accountRepository).delete(any(Account.class));
    }
}
