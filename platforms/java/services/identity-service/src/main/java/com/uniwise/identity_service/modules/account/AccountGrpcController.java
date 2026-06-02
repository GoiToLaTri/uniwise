package com.uniwise.identity_service.modules.account;

import com.uniwise.grpc_spring_boot_starter.annotation.GrpcService;
import com.uniwise.identity.account.v1.AccountServiceGrpc.AccountServiceImplBase;
import com.uniwise.identity.account.v1.AssignRolesRequest;
import com.uniwise.identity.account.v1.AssignRolesResponse;
import com.uniwise.identity.account.v1.RevokeRolesRequest;
import com.uniwise.identity.account.v1.RevokeRolesResponse;
import com.uniwise.identity.account.v1.UpdateAccountRolesRequest;
import com.uniwise.identity.account.v1.UpdateAccountRolesResponse;

import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@GrpcService
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountGrpcController extends AccountServiceImplBase {
    AccountGrpcService accountGrpcService;

    @Override
    public void assignRoles(AssignRolesRequest request,
            StreamObserver<AssignRolesResponse> responseObserver) {
        try {
            AssignRolesResponse response = accountGrpcService.assignRoles(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void revokeRoles(RevokeRolesRequest request,
            StreamObserver<RevokeRolesResponse> responseObserver) {
        try {
            RevokeRolesResponse response = accountGrpcService.revokeRoles(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
