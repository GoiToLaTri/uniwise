package com.uniwise.common.dto.response;

import java.time.Instant;
import java.util.List;
import com.uniwise.common.enums.ECourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseResponse {
    private String id;
    private String publicId;
    private String creatorId;
    private String priceTierId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String thumbnailName;
    private ECourseStatus status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SectionResponse> sections;
    private Boolean isEnrolled;
    private Double progressPercentage;
    private Integer completedLessonsCount;
    private Integer totalLessonsCount;
    
    // New fields
    private Integer studentCount;
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalSections;
    private Integer totalLessons;
}
