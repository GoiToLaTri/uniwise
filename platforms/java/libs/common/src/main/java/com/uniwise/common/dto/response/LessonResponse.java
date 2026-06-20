package com.uniwise.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LessonResponse {
    private String id;
    private String publicId;
    private String sectionId;
    private String title;
    private String lessonType;
    private String contentReference;
    private String status;
    private Integer sortOrder;
}
