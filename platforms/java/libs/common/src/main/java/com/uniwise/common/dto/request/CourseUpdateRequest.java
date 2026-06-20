package com.uniwise.common.dto.request;

import com.uniwise.common.enums.ECourseStatus;

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
public class CourseUpdateRequest {

    String title;
    String description;
    String thumbnailUrl;
    String priceTierId; // Can be set to null for free courses
    ECourseStatus status;
}
