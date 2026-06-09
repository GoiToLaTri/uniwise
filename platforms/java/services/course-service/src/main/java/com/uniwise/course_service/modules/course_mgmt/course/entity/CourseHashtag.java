package com.uniwise.course_service.modules.course_mgmt.course.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.io.Serializable;

import com.uniwise.course_service.modules.hashtag.entity.Hashtag;
 
@Entity
@Table(name = "course_hashtags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(CourseHashtag.CourseHashtagId.class)
public class CourseHashtag {
 
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
 
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;
 
    // ── Composite Key ──────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class CourseHashtagId implements Serializable {
        private String course;
        private String hashtag;
    }
}