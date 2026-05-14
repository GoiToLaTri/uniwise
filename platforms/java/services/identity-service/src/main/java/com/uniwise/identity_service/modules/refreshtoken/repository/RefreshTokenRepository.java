package com.uniwise.identity_service.modules.refreshtoken.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uniwise.identity_service.modules.refreshtoken.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Có thể thêm phương thức xóa các token cũ của 1 session nếu cần
    void deleteBySessionId(String sessionId);
}
