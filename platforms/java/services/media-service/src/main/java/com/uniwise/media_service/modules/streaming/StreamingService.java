package com.uniwise.media_service.modules.streaming;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface StreamingService {
    ResponseEntity<Resource> streamVideo(String lessonId, String filename);
}
