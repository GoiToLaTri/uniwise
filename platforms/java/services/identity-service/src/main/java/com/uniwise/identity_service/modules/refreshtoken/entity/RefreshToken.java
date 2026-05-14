package com.uniwise.identity_service.modules.refreshtoken.entity;

import java.time.Instant;

import com.uniwise.identity_service.modules.session.entity.Session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String tokenHash; // Lưu bản băm thay vì text thuần

    @Column(nullable = false)
    private Instant expiryDate;

    private boolean used; // Đã từng được dùng để refresh chưa?
    private boolean compromised; // Đánh dấu nếu phát hiện nghi vấn bị lộ

    @ManyToOne
    private Session session; // Link ngược lại session
}
