package com.uniwise.course_service.modules.course_mgmt.course.grpc;

import org.springframework.transaction.annotation.Transactional;

import com.uniwise.course.v1.CourseGrpcServiceGrpc.CourseGrpcServiceImplBase;
import com.uniwise.course.v1.GetCoursePriceRequest;
import com.uniwise.course.v1.GetCoursePriceResponse;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.CourseService;
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
}
