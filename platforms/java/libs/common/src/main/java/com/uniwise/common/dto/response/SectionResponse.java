package com.uniwise.common.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SectionResponse {
    private String id;
    private String publicId;
    private String courseId;
    private String title;
    private Integer sortOrder;
    private List<LessonResponse> lessons;
}
