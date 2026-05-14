package com.uniwise.identity_service.modules.session.entity;

import java.time.Instant;

import com.uniwise.identity_service.modules.account.entity.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false, unique = true)
    String token; // Access token

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    Account account;

    // Thay vì lưu cả token, chỉ lưu mã định danh của RefreshToken để mapping
    @Column(name = "refresh_token_id")
    String refreshTokenId;

    // Metadata chi tiết để user kiểm tra lịch sử đăng nhập
    String os; // Windows, iOS, Android
    String browser; // Chrome, Safari

    @Column(name = "device_type")
    String deviceType; // Mobile, Desktop, Tablet

    @Column(name = "ip_address")
    String ipAddress;

    @Column(name = "expires_at")
    Instant expiresAt; // Thời điểm session hết hạn hoàn toàn

    @Column(name = "last_activity")
    Instant lastActivity;

    @Column(name = "access_token_expires_at")
    Instant accessTokenExpiresAt;

    @Column(name = "is_revoked")
    boolean isRevoked; // Đánh dấu session bị quản trị viên hoặc user khóa
}
