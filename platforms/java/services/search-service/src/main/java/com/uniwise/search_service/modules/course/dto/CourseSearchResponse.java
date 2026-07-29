package com.uniwise.search_service.modules.course.dto;

import com.uniwise.common.dto.response.InstructorSummaryResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchResponse {
    private String id;
    private String publicId;
    private String title;
    private String description;
    private InstructorSummaryResponse instructor;
    private String status;
    private String thumbnailUrl;
    private String priceTierId;
    private Integer studentCount;
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalLessons;
    private Integer totalSections;
}
