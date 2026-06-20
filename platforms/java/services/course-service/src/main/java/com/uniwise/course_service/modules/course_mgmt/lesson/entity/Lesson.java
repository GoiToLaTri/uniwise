package com.uniwise.course_service.modules.course_mgmt.lesson.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.util.List;

import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.learning_progress.entity.UserLesson;

@Entity
@Table(name = "lessons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lesson {
 
    // ── Enums ──────────────────────────────────────────────────────────────────
    public enum LessonType {
        VIDEO, QUIZ
    }
 
    public enum LessonStatus {
        PROCESSING, READY
    }
 
    // ── Fields ─────────────────────────────────────────────────────────────────
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
 
    @Column(name = "public_id", unique = true, nullable = false, length = 16)
    private String publicId;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;
 
    @Column(name = "title", nullable = false)
    private String title;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 10)
    private LessonType lessonType;
 
    /**
     * UUID của Video trên MinIO (nếu VIDEO)
     * hoặc ID của Quiz (nếu QUIZ)
     */
    @Column(name = "content_reference", nullable = false, length = 36)
    private String contentReference;
 
    /**
     * Trạng thái async: PROCESSING (đang encode) → READY (sẵn sàng xem)
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private LessonStatus status = LessonStatus.PROCESSING;
 
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
 
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserLesson> userLessons;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
