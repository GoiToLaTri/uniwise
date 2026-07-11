package com.uniwise.platform_event_contract.event.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreatedEvent {
    private String id;
    private String publicId;
    private String title;
    private String description;
    private String creatorId;
    private String status;
    private String thumbnailUrl;
    private String priceTierId;
}
