package com.uniwise.user_service.modules.profile;

import com.uniwise.grpc_spring_boot_starter.annotation.GrpcService;
import com.uniwise.common.exception.HttpException;
import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdRequest;
import com.uniwise.user.profile.v1.GetPublicProfileByAccountIdResponse;
import com.uniwise.user.profile.v1.ProfileServiceGrpc.ProfileServiceImplBase;

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
public class ProfileGrpcController extends ProfileServiceImplBase {
    ProfileGrpcService profileGrpcService;

    @Override
    public void createProfile(CreateProfileRequest request, StreamObserver<CreateProfileResponse> responseObserver) {
        // 1. Gọi xuống lớp nghiệp vụ để xử lý
        CreateProfileResponse response = profileGrpcService.create(request);

        // 2. Trả kết quả về cho Client qua StreamObserver
        responseObserver.onNext(response);

        // 3. Kết thúc lời gọi hàm thành công
        responseObserver.onCompleted();
    }

    @Override
    public void getPublicProfileByAccountId(
            GetPublicProfileByAccountIdRequest request,
            StreamObserver<GetPublicProfileByAccountIdResponse> responseObserver) {
        if (request.getAccountId().isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("account_id is required")
                    .asRuntimeException());
            return;
        }

        try {
            GetPublicProfileByAccountIdResponse response =
                    profileGrpcService.getPublicProfileByAccountId(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (HttpException exception) {
            log.warn("Public profile was not found for the requested account");
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Public profile not found")
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("Failed to fetch public profile through gRPC", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }
}
