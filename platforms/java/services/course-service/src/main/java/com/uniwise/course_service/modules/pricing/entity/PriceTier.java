package com.uniwise.course_service.modules.pricing.entity;

import jakarta.persistence.*;
import lombok.*;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
 
@Entity
@Table(name = "price_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceTier {
 
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;
 
    @Column(name = "tier_name", nullable = false)
    private String tierName;
 
    @Column(name = "price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceAmount;
 
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Builder.Default
    @Column(name = "course_count", nullable = false)
    private Integer courseCount = 0;

    @OneToMany(mappedBy = "priceTier", fetch = FetchType.LAZY)
    private List<Course> courses;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
