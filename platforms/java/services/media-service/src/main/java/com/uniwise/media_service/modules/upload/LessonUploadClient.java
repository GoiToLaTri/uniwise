package com.uniwise.media_service.modules.upload;

import com.uniwise.course.v1.CheckLessonUploadAuthorizationResponse;

public interface LessonUploadClient {
    CheckLessonUploadAuthorizationResponse checkAuthorization(String accountId, String lessonId);
}
