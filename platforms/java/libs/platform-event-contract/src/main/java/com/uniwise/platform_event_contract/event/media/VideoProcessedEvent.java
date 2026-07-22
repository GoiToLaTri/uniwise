package com.uniwise.platform_event_contract.event.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessedEvent {
    private String lessonId;
    private String bucketName;
    private String status; // SUCCESS or FAILED
    private Long durationMillis;
}
