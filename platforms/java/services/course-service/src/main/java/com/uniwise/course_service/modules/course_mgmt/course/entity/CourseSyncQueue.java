package com.uniwise.course_service.modules.course_mgmt.course.entity;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_sync_queue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseSyncQueue {

    @Id
    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
