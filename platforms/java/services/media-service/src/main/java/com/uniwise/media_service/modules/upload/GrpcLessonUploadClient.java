package com.uniwise.media_service.modules.upload;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.uniwise.course.v1.CheckLessonUploadAuthorizationRequest;
import com.uniwise.course.v1.CheckLessonUploadAuthorizationResponse;
import com.uniwise.course.v1.CourseGrpcServiceGrpc.CourseGrpcServiceBlockingStub;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GrpcLessonUploadClient implements LessonUploadClient {
    static final long AUTHORIZATION_TIMEOUT_SECONDS = 3;

    @NonFinal
    @GrpcClient("course-service")
    CourseGrpcServiceBlockingStub courseServiceClient;

    @Override
    public CheckLessonUploadAuthorizationResponse checkAuthorization(String accountId, String lessonId) {
        CheckLessonUploadAuthorizationRequest request = CheckLessonUploadAuthorizationRequest.newBuilder()
                .setAccountId(accountId)
                .setLessonId(lessonId)
                .build();

        return courseServiceClient
                .withDeadlineAfter(AUTHORIZATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .checkLessonUploadAuthorization(request);
    }
}
