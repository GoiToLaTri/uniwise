package com.uniwise.common.dto.request;

import com.uniwise.common.enums.ECourseStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseCreateRequest {

    @NotBlank(message = "COURSE_TITLE_REQUIRED")
    String title;

    String description;

    String thumbnailUrl;

    String thumbnailName;

    String priceTierId; // Optional/nullable for free courses

    ECourseStatus status; // Defaults to DRAFT in service if null
}
