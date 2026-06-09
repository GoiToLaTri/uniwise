package com.uniwise.course_service.modules.course_mgmt.course.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.time.Instant;
import java.util.List;

import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;
import com.uniwise.course_service.modules.pricing.entity.PriceTier;
 
@Entity
@Table(name = "courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
 
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
 
    // Nullable: null nếu khóa học Miễn phí
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_tier_id", nullable = true)
    private PriceTier priceTier;
 
    @Column(name = "title", nullable = false)
    private String title;
 
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
 
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
 
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Section> sections;
 
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CourseHashtag> courseHashtags;
 
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserCourse> userCourses;
 
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}