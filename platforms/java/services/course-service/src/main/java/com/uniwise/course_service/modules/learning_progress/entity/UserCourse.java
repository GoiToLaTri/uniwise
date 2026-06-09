package com.uniwise.course_service.modules.learning_progress.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.io.Serializable;
import java.time.Instant;

import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
 
@Entity
@Table(name = "user_courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(UserCourse.UserCourseId.class)
public class UserCourse {
 
    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId; // Từ Auth Context — không FK sang service khác
 
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
 
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;
 
    @Builder.Default
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;
 
    @PrePersist
    protected void onCreate() {
        this.enrolledAt = Instant.now();
    }
 
    // ── Composite Key ──────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class UserCourseId implements Serializable {
        private String userId;
        private String course;
    }
}
