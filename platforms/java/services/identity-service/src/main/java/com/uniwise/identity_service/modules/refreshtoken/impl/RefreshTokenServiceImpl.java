package com.uniwise.identity_service.modules.refreshtoken.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.utils.TokenUtils;
import com.uniwise.identity_service.modules.refreshtoken.RefreshTokenService;
import com.uniwise.identity_service.modules.refreshtoken.entity.RefreshToken;
import com.uniwise.identity_service.modules.refreshtoken.repository.RefreshTokenRepository;
import com.uniwise.identity_service.modules.session.entity.Session;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {
    RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public RefreshToken create(Session session, String rawToken) {
        // Thời gian sống của Refresh Token (ví dụ: 7 ngày)
        Instant expiryDate = Instant.now().plus(7, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(TokenUtils.hash(rawToken))
                .expiryDate(expiryDate)
                .session(session)
                .used(false)
                .compromised(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken getByHash(String hash) {
        return refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new HttpException(AuthError.TOKEN_INVALID));
    }

    @Override
    public RefreshToken getByHashForUpdate(String hash) {
        return refreshTokenRepository.findByTokenHashForUpdate(hash)
                .orElseThrow(() -> new HttpException(AuthError.TOKEN_INVALID));
    }

    @Override
    @Transactional
    public void markUsed(RefreshToken token) {
        token.setUsed(true);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void markCompromised(RefreshToken token) {
        token.setCompromised(true);
        // Khi một token trong chuỗi bị lộ, ta nên đánh dấu toàn bộ session bị thu hồi
        if (token.getSession() != null)
            token.getSession().setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        refreshTokenRepository.deleteBySessionId(sessionId);        
    }
}
