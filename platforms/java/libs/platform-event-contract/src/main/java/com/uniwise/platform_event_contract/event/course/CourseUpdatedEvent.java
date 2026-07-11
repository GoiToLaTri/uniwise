package com.uniwise.platform_event_contract.event.course;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseUpdatedEvent {
    private String id;
    private String publicId;
    private String title;
    private String description;
    private String status;
    private String thumbnailUrl;
    private String priceTierId;
    private Instant updatedAt;
}
