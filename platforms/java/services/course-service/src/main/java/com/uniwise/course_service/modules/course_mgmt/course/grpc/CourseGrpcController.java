package com.uniwise.course_service.modules.course_mgmt.course.grpc;

import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import com.uniwise.course.v1.CheckLessonUploadAuthorizationRequest;
import com.uniwise.course.v1.CheckLessonUploadAuthorizationResponse;
import com.uniwise.course.v1.CourseGrpcServiceGrpc.CourseGrpcServiceImplBase;
import com.uniwise.course.v1.GetCoursePriceRequest;
import com.uniwise.course.v1.GetCoursePriceResponse;
import com.uniwise.course.v1.CheckLessonAccessRequest;
import com.uniwise.course.v1.CheckLessonAccessResponse;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;
import com.uniwise.course_service.modules.course_mgmt.course.CourseService;
import com.uniwise.course_service.modules.course_mgmt.lesson.LessonService;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcService;
import com.uniwise.common.exception.HttpException;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@GrpcService
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseGrpcController extends CourseGrpcServiceImplBase {
    CourseService courseService;
    LessonService lessonService;
    LearningProgressService learningProgressService;

    @Override
    @Transactional(readOnly = true)
    public void getCoursePrice(GetCoursePriceRequest request, StreamObserver<GetCoursePriceResponse> responseObserver) {
        try {
            log.info("gRPC: Fetching price for course ID: {}", request.getCourseId());
            Course course;
            try {
                course = courseService.getEntityById(request.getCourseId());
            } catch (HttpException e) {
                log.warn("gRPC: Course not found with ID: {}", request.getCourseId());
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Course not found")
                        .asRuntimeException());
                return;
            }

            long price = 0L;
            if (course.getPriceTier() != null && course.getPriceTier().getPriceAmount() != null) {
                price = course.getPriceTier().getPriceAmount().longValue();
            }

            GetCoursePriceResponse response = GetCoursePriceResponse.newBuilder()
                    .setCourseId(course.getId())
                    .setPrice(price)
                    .setTitle(course.getTitle() != null ? course.getTitle() : "")
                    .setIsActive(Boolean.TRUE.equals(course.getIsActive()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Error: Failed to fetch course price", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkLessonAccess(CheckLessonAccessRequest request, StreamObserver<CheckLessonAccessResponse> responseObserver) {
        try {
            log.info("gRPC: Checking lesson access for account {} and lesson {}", request.getAccountId(), request.getLessonId());
            
            Lesson lesson;
            try {
                lesson = lessonService.getEntityByPublicId(request.getLessonId());
            } catch (HttpException e) {
                log.warn("gRPC: Lesson not found with ID: {}", request.getLessonId());
                responseObserver.onNext(CheckLessonAccessResponse.newBuilder().setHasAccess(false).build());
                responseObserver.onCompleted();
                return;
            }

            boolean hasAccess = false;
            
            // Condition 1: Free preview lesson
            if (Boolean.TRUE.equals(lesson.getIsPreview())) {
                hasAccess = true;
            } else {
                String courseId = lesson.getSection().getCourse().getId();
                
                // Condition 2: User is enrolled
                hasAccess = learningProgressService.isEnrolled(request.getAccountId(), courseId);
                
                // Condition 3: Check if user is the course creator
                if (!hasAccess) {
                    if (request.getAccountId().equals(lesson.getSection().getCourse().getCreatorId())) {
                        hasAccess = true;
                    }
                }
            }

            responseObserver.onNext(CheckLessonAccessResponse.newBuilder().setHasAccess(hasAccess).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC Error: Failed to check lesson access", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkLessonUploadAuthorization(
            CheckLessonUploadAuthorizationRequest request,
            StreamObserver<CheckLessonUploadAuthorizationResponse> responseObserver) {
        try {
            log.info("gRPC: Checking upload authorization for account {} and lesson {}",
                    request.getAccountId(), request.getLessonId());

            Lesson lesson;
            try {
                lesson = lessonService.getEntityByPublicId(request.getLessonId());
            } catch (HttpException e) {
                log.warn("gRPC: Upload target lesson not found with public ID: {}", request.getLessonId());
                responseObserver.onNext(CheckLessonUploadAuthorizationResponse.newBuilder()
                        .setLessonExists(false)
                        .build());
                responseObserver.onCompleted();
                return;
            }

            boolean isOwner = Objects.equals(
                    lesson.getSection().getCourse().getCreatorId(),
                    request.getAccountId());
            boolean isVideoLesson = lesson.getLessonType() == Lesson.LessonType.VIDEO;

            responseObserver.onNext(CheckLessonUploadAuthorizationResponse.newBuilder()
                    .setLessonExists(true)
                    .setIsOwner(isOwner)
                    .setIsVideoLesson(isVideoLesson)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Unexpected system error while checking lesson upload authorization", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error")
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
