package com.uniwise.identity_service.modules.authentication.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.uniwise.common.dto.request.GetTokenRequest;
import com.uniwise.common.dto.request.RefreshTokenRequest;
import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.common.dto.response.TokenResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.utils.ServletUtils;
import com.uniwise.common.utils.TokenUtils;
import com.uniwise.identity_service.modules.account.AccountService;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.authentication.AuthenticationService;
import com.uniwise.identity_service.modules.authentication.data.RedisToken;
import com.uniwise.identity_service.modules.permission.entity.Permission;
import com.uniwise.identity_service.modules.redis.RedisService;
import com.uniwise.identity_service.modules.refreshtoken.RefreshTokenService;
import com.uniwise.identity_service.modules.refreshtoken.entity.RefreshToken;
import com.uniwise.identity_service.modules.session.SessionService;
import com.uniwise.identity_service.modules.session.entity.Session;
import com.uniwise.identity_service.modules.session.mapper.SessionMapper;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements AuthenticationService {
        AccountService accountService;
        SessionService sessionService;
        RefreshTokenService refreshTokenService;
        SessionMapper sessionMapper;
        RedisService redisService;
        PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public TokenResponse getToken(GetTokenRequest request) {
                // 1. Lấy Account
                Account account = accountService.getByEmail(request.getEmail());

                // 2. Kiểm tra mật khẩu
                if (!passwordEncoder.matches(request.getPassword(), account.getPassword()))
                        throw new HttpException(AuthError.INVALID_CREDENTIALS);

                // Only reveal the disabled state after the password has been verified.
                if (!Boolean.TRUE.equals(account.getIsActive()))
                        throw new HttpException(AuthError.ACCOUNT_DISABLED);

                // 3. TỰ ĐỘNG lấy thông tin từ Request thông qua ServletUtils
                // String userAgent = ServletUtils.getUserAgent();
                String ipAddress = ServletUtils.getRemoteAddress();

                String os = "Unknown OS";
                String browser = "Unknown Browser";
                String deviceType = "Unknown Device Type";
                // Dùng làm accesstoken
                String token = TokenUtils.generateRandomToken().replaceAll("-", "");
                String hashedToken = TokenUtils.hash(token);

                // 4. Khởi tạo Session
                Session session = Session.builder()
                                .account(account)
                                .os(os)
                                .browser(browser)
                                .deviceType(deviceType)
                                .ipAddress(ipAddress)
                                // Session sẽ có thời hạn 30 ngày
                                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                                // Mỗi lần đăng nhập mới sẽ cập nhật lastActivity, khi nào user logout hoặc
                                // session bị revoke thì sẽ không update nữa
                                .lastActivity(Instant.now())
                                .isRevoked(false)
                                .token(hashedToken)
                                // Access Token sẽ có thời hạn 15 phút
                                .accessTokenExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                                .build();

                sessionService.create(session);
                String scope = buildScope(account);

                RedisToken redisToken = RedisToken.builder()
                                .sessionId(session.getId())
                                .accountId(account.getId())
                                .expiresAt(session.getAccessTokenExpiresAt().toEpochMilli())
                                .scope(scope)
                                .build();

                // Tính TTL cho Redis dựa trên thời gian còn lại của Access Token
                Duration duration = Duration.between(
                                Instant.now(),
                                session.getAccessTokenExpiresAt());

                redisService.<RedisToken>setKey("access:" + hashedToken, redisToken,
                                duration.toMillis(),
                                TimeUnit.MILLISECONDS);
                // 5. Tạo Refresh Token và Access Token
                String rawRefreshToken = TokenUtils.generateRandomToken().replaceAll("-", "");
                RefreshToken refreshToken = refreshTokenService.create(session,
                                rawRefreshToken);

                session.setRefreshTokenId(refreshToken.getId());
                sessionService.update(session);

                SessionResponse sessionResponse = sessionMapper.toResponse(session);
                Duration re = Duration.between(Instant.now(), session.getExpiresAt());
                redisService.<SessionResponse>setKey("session:" + sessionResponse.getId(),
                                sessionResponse, re.toMillis(),
                                TimeUnit.MILLISECONDS);

                return TokenResponse.builder()
                                .accessToken(token)
                                .refreshToken(rawRefreshToken)
                                .sessionId(session.getId())
                                .scope(scope)
                                .expiresAt(session.getAccessTokenExpiresAt())
                                .build();

        }

        @Override
        public List<SessionResponse> getActiveSessions(String accountId, String currentSessionId) {
                // TODO Auto-generated method stub
                return null;
        }

        @Override
        @Transactional
        public void logout(RefreshTokenRequest request) {
                // 1. Tìm Refresh Token qua Hash
                String hashedToken = TokenUtils.hash(request.getRefreshToken());
                RefreshToken refreshToken = refreshTokenService.getByHash(hashedToken);

                Session session = refreshToken.getSession();
                if (session == null || session.isRevoked())
                        throw new HttpException(AuthError.SESSION_NOT_FOUND);

                // 2. Thu hồi session và xóa token liên quan
                session.setRevoked(true);
                sessionService.update(session);

                redisService.deleteKey("access:" + session.getToken());
                redisService.deleteKey("session:" + session.getId());

                refreshTokenService.deleteBySessionId(session.getId());
        }

        @Override
        public void revokeSession(String sessionId) {
                // TODO Auto-generated method stub

        }

        @Override
        @Transactional(dontRollbackOn = HttpException.class)
        public TokenResponse refresh(RefreshTokenRequest request) {
                Instant now = Instant.now();
                String hashedToken = TokenUtils.hash(request.getRefreshToken());
                // Serialize refresh attempts for the same token to prevent two concurrent
                // requests from both rotating it successfully.
                RefreshToken oldToken = refreshTokenService.getByHashForUpdate(hashedToken);

                Session session = oldToken.getSession();
                if (session == null)
                        throw new HttpException(AuthError.SESSION_NOT_FOUND);

                // A compromised token or a previously consumed token invalidates its session.
                if (oldToken.isCompromised() || oldToken.isUsed()) {
                        compromiseSession(oldToken, session);
                        log.error("Security alert: Refresh token reuse detected. Session: {}", session.getId());
                        throw new HttpException(AuthError.TOKEN_COMPROMISED);
                }

                if (oldToken.getExpiryDate() == null || !oldToken.getExpiryDate().isAfter(now))
                        throw new HttpException(AuthError.TOKEN_EXPIRED);

                if (session.isRevoked()
                                || session.getExpiresAt() == null
                                || !session.getExpiresAt().isAfter(now))
                        throw new HttpException(AuthError.SESSION_NOT_FOUND);

                // All validation has passed; invalidate the old access token and consume the
                // refresh token exactly once.
                redisService.deleteKey("access:" + session.getToken());
                refreshTokenService.markUsed(oldToken);

                String accessToken = TokenUtils.generateRandomToken().replaceAll("-", "");
                String newRawToken = TokenUtils.generateRandomToken().replaceAll("-", "");
                RefreshToken newToken = refreshTokenService.create(session, newRawToken);
                String scope = buildScope(session.getAccount());
                Instant accessTokenExpiresAt = now.plus(15, ChronoUnit.MINUTES);

                RedisToken redisToken = RedisToken.builder()
                                .sessionId(session.getId())
                                .accountId(session.getAccount().getId())
                                .expiresAt(accessTokenExpiresAt.toEpochMilli())
                                .scope(scope)
                                .build();

                Duration duration = Duration.between(Instant.now(), accessTokenExpiresAt);

                session.setToken(TokenUtils.hash(accessToken));
                session.setRefreshTokenId(newToken.getId());
                session.setLastActivity(now);
                session.setAccessTokenExpiresAt(accessTokenExpiresAt);
                sessionService.update(session);

                redisService.setKey("access:" + TokenUtils.hash(accessToken), redisToken,
                                duration.toMillis(), TimeUnit.MILLISECONDS);

                SessionResponse sessionResponse = sessionMapper.toResponse(session);
                Duration sessionDuration = Duration.between(Instant.now(), session.getExpiresAt());
                redisService.setKey("session:" + session.getId(), sessionResponse,
                                sessionDuration.toMillis(), TimeUnit.MILLISECONDS);

                return TokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(newRawToken)
                                .sessionId(session.getId())
                                .expiresAt(session.getAccessTokenExpiresAt())
                                .scope(scope)
                                .build();
        }

        private void compromiseSession(RefreshToken token, Session session) {
                refreshTokenService.markCompromised(token);
                session.setRevoked(true);
                sessionService.update(session);
                redisService.deleteKey("access:" + session.getToken());
                redisService.deleteKey("session:" + session.getId());
        }

        private String buildScope(Account account) {
                Set<String> roles = account.getRoles().stream()
                                .map(role -> "ROLE_" + role.getName())
                                .collect(Collectors.toSet());

                Set<String> permissions = account.getRoles().stream()
                                .flatMap(role -> role.getPermissions().stream())
                                .map(Permission::getName)
                                .collect(Collectors.toSet());

                String scope = String.join(" ", roles) + " " + String.join(" ", permissions);
                return scope.trim();
        }
}
