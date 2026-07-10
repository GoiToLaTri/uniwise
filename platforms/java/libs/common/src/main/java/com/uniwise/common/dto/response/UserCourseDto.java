package com.uniwise.common.dto.response;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCourseDto {
    String courseId;
    String publicId;
    String title;
    String thumbnail;
    Instant enrolledAt;
    Boolean isPaid;
    Double progressPercentage;
}
