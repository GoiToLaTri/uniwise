package com.uniwise.course_service.modules.hashtag.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.time.Instant;
import java.util.List;

import com.uniwise.course_service.modules.course_mgmt.course.entity.CourseHashtag;
 
@Entity
@Table(name = "hashtags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hashtag {
 
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
 
    @Column(name = "name", nullable = false, unique = true)
    private String name; // Chuẩn hóa chữ thường trước khi lưu
 
    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false; // Admin duyệt làm tag hệ thống
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
 
    @OneToMany(mappedBy = "hashtag", fetch = FetchType.LAZY)
    private List<CourseHashtag> courseHashtags;
 
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.name != null) {
            this.name = this.name.toLowerCase();
        }
    }
}
