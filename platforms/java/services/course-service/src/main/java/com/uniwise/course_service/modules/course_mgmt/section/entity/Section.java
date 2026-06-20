package com.uniwise.course_service.modules.course_mgmt.section.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.util.List;

import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
 
@Entity
@Table(name = "sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Section {
 
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "public_id", unique = true, nullable = false, length = 16)
    private String publicId;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
 
    @Column(name = "title", nullable = false)
    private String title;
 
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
 
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<Lesson> lessons;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}