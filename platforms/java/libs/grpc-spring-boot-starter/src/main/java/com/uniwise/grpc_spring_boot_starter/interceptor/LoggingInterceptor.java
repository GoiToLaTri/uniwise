package com.uniwise.grpc_spring_boot_starter.interceptor;

import org.springframework.util.StopWatch;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        log.info("gRPC Call: {}", methodName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                stopWatch.stop();
                log.info("gRPC Finished: {} with status: {} in {}ms",
                        methodName, status.getCode(), stopWatch.getTotalTimeMillis());
                super.close(status, trailers);
            }
        }, headers);
    }
}
