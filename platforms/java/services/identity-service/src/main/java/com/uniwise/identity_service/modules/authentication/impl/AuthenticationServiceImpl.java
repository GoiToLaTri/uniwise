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
        @Transactional
        public TokenResponse refresh(RefreshTokenRequest request) {
                // 1. Tìm Refresh Token qua Hash (RefreshTokenService xử lý hash bên trong hoặc
                // gọi Util)
                String hashedToken = TokenUtils.hash(request.getRefreshToken());
                RefreshToken oldToken = refreshTokenService.getByHash(hashedToken);

                Session session = oldToken.getSession();
                redisService.deleteKey("access:" + session.getToken());
                // 2. Kiểm tra tính hợp lệ session qua Session logic
                if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now()))
                        throw new HttpException(AuthError.SESSION_NOT_FOUND);

                // 3. Phát hiện sử dụng lại (Reuse Detection)
                if (oldToken.isUsed()) {
                        refreshTokenService.markCompromised(oldToken);
                        // session.setRevoked(true);
                        sessionService.update(session);
                        // TODO: Logic hiện tại thu hồi 1 session khi phát hiện reuse, có thể nâng cấp
                        // thành thu hồi toàn bộ các session của user nếu muốn tăng cường bảo mật
                        // TODO: Dùng message queue gửi message cho người dùng
                        // Ghi log chi tiết để phục vụ điều tra sau này
                        log.error("Security alert: Token reuse detected! Session: {}", session.getId());
                        throw new HttpException(AuthError.TOKEN_COMPROMISED);
                }

                // 4. Đánh dấu đã sử dụng thông qua Service
                refreshTokenService.markUsed(oldToken);

                // 5. Xoay vòng Token (Rotation)
                String accessToken = TokenUtils.generateRandomToken().replaceAll("-", "");
                String newRawToken = TokenUtils.generateRandomToken().replaceAll("-", "");
                RefreshToken newToken = refreshTokenService.create(session, newRawToken);
                String scope = buildScope(session.getAccount());
                // Lưu thông tin token mới vào Redis để phục vụ xác thực sau này
                RedisToken redisToken = RedisToken.builder()
                                .sessionId(session.getId())
                                .accountId(session.getAccount().getId())
                                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES).toEpochMilli())
                                .scope(scope)
                                .build();

                Instant accessTokenExpiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
                Duration duration = Duration.between(Instant.now(), accessTokenExpiresAt);

                // 6. Cập nhật Session metadata
                session.setToken(TokenUtils.hash(accessToken));
                session.setRefreshTokenId(newToken.getId());
                session.setLastActivity(Instant.now());
                sessionService.update(session);
                session.setAccessTokenExpiresAt(accessTokenExpiresAt);

                redisService.setKey("access:" + TokenUtils.hash(accessToken), redisToken,
                                duration.toMillis(), TimeUnit.MILLISECONDS);

                return TokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(newRawToken)
                                .sessionId(session.getId())
                                .expiresAt(session.getAccessTokenExpiresAt())
                                .scope(scope)
                                .build();
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
