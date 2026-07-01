package com.uniwise.media_service.modules.upload;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uniwise.common.dto.response.ApiResponse;
import com.uniwise.common.dto.response.UploadResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadController {

    UploadService uploadService;

    @PostMapping(value = "/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadResponse> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<UploadResponse>builder()
                .code("CREATED")
                .message("Thumbnail uploaded successfully")
                .data(uploadService.uploadThumbnail(file))
                .build();
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("lessonId") String lessonId) {
        return ApiResponse.<UploadResponse>builder()
                .code("CREATED")
                .message("Video uploaded successfully")
                .data(uploadService.uploadVideo(file, lessonId))
                .build();
    }
}
