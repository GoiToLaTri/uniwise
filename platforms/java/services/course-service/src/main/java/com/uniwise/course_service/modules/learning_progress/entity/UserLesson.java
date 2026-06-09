package com.uniwise.course_service.modules.learning_progress.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.io.Serializable;
import java.time.Instant;

import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
 
@Entity
@Table(name = "user_lessons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(UserLesson.UserLessonId.class)
public class UserLesson {
 
    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId; // Từ Auth Context — không FK sang service khác
 
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
 
    @Builder.Default
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;
 
    /** Số giây video đang xem dở, null nếu chưa bắt đầu */
    @Column(name = "last_watched_position")
    private Integer lastWatchedPosition;
 
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
 
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
 
    // ── Composite Key ──────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class UserLessonId implements Serializable {
        private String userId;
        private String lesson;
    }
}