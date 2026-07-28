package com.uniwise.media_service.modules.upload;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.course.v1.CheckLessonUploadAuthorizationResponse;
import com.uniwise.media_service.modules.upload.error.UploadError;

import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonUploadAuthorizer {
    LessonUploadClient lessonUploadClient;

    public String authorize(String lessonId) {
        if (!StringUtils.hasText(lessonId))
            throw new HttpException(UploadError.LESSON_ID_REQUIRED);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName()))
            throw new HttpException(AuthError.ACCESS_DENIED);

        String normalizedLessonId = lessonId.trim();
        CheckLessonUploadAuthorizationResponse response;
        try {
            response = lessonUploadClient.checkAuthorization(authentication.getName(), normalizedLessonId);
        } catch (StatusRuntimeException e) {
            log.error("Course service failed to validate upload target lesson {}: {}",
                    normalizedLessonId, e.getStatus());
            throw new HttpException(UploadError.LESSON_VALIDATION_UNAVAILABLE);
        } catch (RuntimeException e) {
            log.error("Could not validate upload target lesson {}", normalizedLessonId, e);
            throw new HttpException(UploadError.LESSON_VALIDATION_UNAVAILABLE);
        }

        if (!response.getLessonExists())
            throw new HttpException(UploadError.LESSON_NOT_FOUND);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "admin:all".equals(authority.getAuthority()));
        if (!response.getIsOwner() && !isAdmin)
            throw new HttpException(AuthError.ACCESS_DENIED);

        if (!response.getIsVideoLesson())
            throw new HttpException(UploadError.LESSON_NOT_VIDEO);

        return normalizedLessonId;
    }
}
