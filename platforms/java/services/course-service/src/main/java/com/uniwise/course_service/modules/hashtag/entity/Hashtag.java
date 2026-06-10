package com.uniwise.course_service.modules.hashtag.entity;

import java.time.Instant;
import java.util.List;

import com.uniwise.course_service.modules.course_mgmt.course.entity.CourseHashtag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
 
@Entity
@Table(name = "hashtags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Hashtag {
 
    // ===== PRIMARY KEY =====
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
 
    // ===== UNIQUE BUSINESS IDENTIFIER =====
    @Column(name = "name", nullable = false, unique = true)
    String name; // Chuẩn hóa chữ thường trước khi lưu
 
    // ===== STATUS FIELD =====
    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    Boolean isVerified = false; // Admin duyệt làm tag hệ thống
 
    // ===== COUNTER FIELD =====
    @Builder.Default
    @Column(name = "course_count", nullable = false)
    Integer courseCount = 0;
 
    // ===== AUDIT FIELDS =====
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
 
    // ===== RELATIONSHIPS =====
    @OneToMany(mappedBy = "hashtag", fetch = FetchType.LAZY)
    List<CourseHashtag> courseHashtags;
 
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.name != null) {
            this.name = this.name.toLowerCase();
        }
    }
}
