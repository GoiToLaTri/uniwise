package com.uniwise.ffmpeg_worker.service;

import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;

public interface VideoTranscodeService {
    /**
     * Transcodes an uploaded video to HLS format (M3U8 and TS segments) and uploads it back to MinIO.
     * 
     * @param envelope the event envelope containing VideoUploadedEvent
     */
    void transcodeVideoToHls(EventEnvelope<VideoUploadedEvent> envelope);
}
