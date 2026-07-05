package com.uniwise.media_service.modules.streaming;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.course.v1.CheckLessonAccessRequest;
import com.uniwise.course.v1.CheckLessonAccessResponse;
import com.uniwise.course.v1.CourseGrpcServiceGrpc.CourseGrpcServiceBlockingStub;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/streaming")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StreamingController {

    StreamingService streamingService;

    @NonFinal
    @GrpcClient("course-service")
    CourseGrpcServiceBlockingStub courseServiceClient;

    @GetMapping("/lessons/{lessonId}/{filename}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String lessonId,
            @PathVariable String filename) {
        
        String accountId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Checking lesson access for account {} and lesson {}", accountId, lessonId);

        try {
            CheckLessonAccessRequest request = CheckLessonAccessRequest.newBuilder()
                    .setAccountId(accountId)
                    .setLessonId(lessonId)
                    .build();
            CheckLessonAccessResponse response = courseServiceClient.checkLessonAccess(request);
            
            if (!response.getHasAccess()) {
                log.warn("Account {} does not have access to lesson {}", accountId, lessonId);
                throw new HttpException(AuthError.ACCESS_DENIED); 
            }
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to check lesson access via gRPC", e);
            throw new HttpException(AuthError.ACCESS_DENIED); 
        }
        
        return streamingService.streamVideo(lessonId, filename);
    }
}
