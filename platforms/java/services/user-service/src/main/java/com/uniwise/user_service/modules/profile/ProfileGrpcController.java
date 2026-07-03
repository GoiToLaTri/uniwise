package com.uniwise.user_service.modules.profile;

import com.uniwise.grpc_spring_boot_starter.annotation.GrpcService;
import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;
import com.uniwise.user.profile.v1.ProfileServiceGrpc.ProfileServiceImplBase;

import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

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
}
