package com.uniwise.platform_event_contract.event.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private String objectKey;
    private String bucketName;
    private String originalFilename;
    private String contentType;
    private Long size;
}
