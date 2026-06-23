package com.uniwise.media_service.modules.upload;

import org.springframework.web.multipart.MultipartFile;
import com.uniwise.common.dto.response.UploadResponse;

public interface UploadService {
    UploadResponse uploadThumbnail(MultipartFile file);
    UploadResponse uploadVideo(MultipartFile file);
}
