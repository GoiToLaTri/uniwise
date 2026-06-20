package com.uniwise.common.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class LessonCreateRequest {

    @NotBlank(message = "LESSON_TITLE_REQUIRED")
    String title;

    @NotBlank(message = "SECTION_ID_REQUIRED")
    String sectionId;

    @NotBlank(message = "LESSON_TYPE_REQUIRED")
    String lessonType; // Maps to Lesson.LessonType (VIDEO, QUIZ)

    @NotBlank(message = "CONTENT_REFERENCE_REQUIRED")
    String contentReference;

    @NotNull(message = "SORT_ORDER_REQUIRED")
    @Min(value = 0, message = "SORT_ORDER_INVALID")
    Integer sortOrder;
}
