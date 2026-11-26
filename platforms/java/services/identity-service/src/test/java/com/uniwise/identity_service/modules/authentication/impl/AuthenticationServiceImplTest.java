package com.uniwise.identity_service.modules.authentication.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.uniwise.common.dto.request.GetTokenRequest;
import com.uniwise.common.dto.request.RefreshTokenRequest;
import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.common.dto.response.TokenResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.identity_service.modules.account.AccountService;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.permission.entity.Permission;
import com.uniwise.identity_service.modules.redis.RedisService;
import com.uniwise.identity_service.modules.refreshtoken.RefreshTokenService;
import com.uniwise.identity_service.modules.refreshtoken.entity.RefreshToken;
import com.uniwise.identity_service.modules.role.entity.Role;
import com.uniwise.identity_service.modules.session.SessionService;
import com.uniwise.identity_service.modules.session.entity.Session;
import com.uniwise.identity_service.modules.session.mapper.SessionMapper;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AccountService accountService;
    @Mock
    private SessionService sessionService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getToken_filtersOutInactiveRolesAndTheirPermissions() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        Permission userPerm = Permission.builder().name("profile:get-by-account-id").build();
        Permission instructorPerm = Permission.builder().name("course:create").build();

        Role activeUserRole = Role.builder()
                .name("USER")
                .isActive(true)
                .permissions(Set.of(userPerm))
                .build();
        Role inactiveInstructorRole = Role.builder()
                .name("INSTRUCTOR")
                .isActive(false)
                .permissions(Set.of(instructorPerm))
                .build();

        Account account = Account.builder()
                .id("acc-1")
                .email("user@uniwise.com")
                .password("encoded")
                .isActive(true)
                .roles(Set.of(activeUserRole, inactiveInstructorRole))
                .build();

        when(accountService.getByEmail("user@uniwise.com")).thenReturn(account);
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(refreshTokenService.create(any(Session.class), anyString()))
                .thenReturn(RefreshToken.builder().id("rt-1").build());
        when(sessionMapper.toResponse(any(Session.class)))
                .thenReturn(SessionResponse.builder().id("sess-1").build());

        GetTokenRequest request = GetTokenRequest.builder()
                .email("user@uniwise.com")
                .password("secret")
                .build();

        TokenResponse response = authenticationService.getToken(request);

        assertTrue(response.getScope().contains("ROLE_USER"));
        assertTrue(response.getScope().contains("profile:get-by-account-id"));
        assertFalse(response.getScope().contains("ROLE_INSTRUCTOR"));
        assertFalse(response.getScope().contains("course:create"));
        verify(redisService, times(2)).setKey(anyString(), any(), anyLong(), any());
    }

    @Test
    void logout_rejectsUsedRefreshTokenWithoutDeletingSessionTokens() {
        Session session = Session.builder().id("sess-1").token("hash-access").isRevoked(false).build();
        RefreshToken usedToken = RefreshToken.builder()
                .id("rt-1")
                .used(true)
                .compromised(false)
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .session(session)
                .build();

        when(refreshTokenService.getByHash(anyString())).thenReturn(usedToken);

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("raw-token").build();
        HttpException ex = assertThrows(HttpException.class, () -> authenticationService.logout(request));

        assertEquals(AuthError.TOKEN_INVALID, ex.getError());
        verifyNoInteractions(sessionService, redisService);
        verify(refreshTokenService, never()).deleteBySessionId(anyString());
    }

    @Test
    void logout_rejectsExpiredRefreshToken() {
        Session session = Session.builder().id("sess-1").token("hash-access").isRevoked(false).build();
        RefreshToken expiredToken = RefreshToken.builder()
                .id("rt-1")
                .used(false)
                .compromised(false)
                .expiryDate(Instant.now().minus(1, ChronoUnit.MINUTES))
                .session(session)
                .build();

        when(refreshTokenService.getByHash(anyString())).thenReturn(expiredToken);

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("raw-token").build();
        HttpException ex = assertThrows(HttpException.class, () -> authenticationService.logout(request));

        assertEquals(AuthError.TOKEN_EXPIRED, ex.getError());
        verifyNoInteractions(sessionService, redisService);
        verify(refreshTokenService, never()).deleteBySessionId(anyString());
    }

    @Test
    void refresh_rejectsDisabledAccount() {
        Account disabledAccount = Account.builder().id("acc-1").isActive(false).build();
        Session session = Session.builder()
                .id("sess-1")
                .account(disabledAccount)
                .isRevoked(false)
                .expiresAt(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();
        RefreshToken validToken = RefreshToken.builder()
                .id("rt-1")
                .used(false)
                .compromised(false)
                .expiryDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .session(session)
                .build();

        when(refreshTokenService.getByHashForUpdate(anyString())).thenReturn(validToken);

        RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("raw-token").build();
        HttpException ex = assertThrows(HttpException.class, () -> authenticationService.refresh(request));

        assertEquals(AuthError.ACCOUNT_DISABLED, ex.getError());
        verify(refreshTokenService, never()).markUsed(any());
        verify(redisService, never()).deleteKey(eq("access:" + session.getToken()));
    }
}
