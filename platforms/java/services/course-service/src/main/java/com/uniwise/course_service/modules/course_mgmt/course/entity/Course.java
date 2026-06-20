package com.uniwise.course_service.modules.course_mgmt.course.entity;

import java.time.Instant;
import java.util.List;

import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.learning_progress.entity.UserCourse;
import com.uniwise.course_service.modules.pricing.entity.PriceTier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Entity
@Table(name = "courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
 
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "creator_id")
    private String creatorId;
 
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

    @Column(name="status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ECourseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name="updated_at")
    private Instant updatedAt;
 
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