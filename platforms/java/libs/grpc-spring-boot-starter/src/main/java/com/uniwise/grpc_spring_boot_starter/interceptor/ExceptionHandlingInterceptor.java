package com.uniwise.grpc_spring_boot_starter.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.ErrorDefinition;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionHandlingInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> ERROR_CODE_KEY = 
            Metadata.Key.of("error-code", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> ERROR_MESSAGE_KEY = 
            Metadata.Key.of("error-message", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> ERROR_STATUS_KEY = 
            Metadata.Key.of("error-status", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        ServerCall.Listener<ReqT> delegate;
        try {
            delegate = next.startCall(call, headers);
        } catch (Exception e) {
            handleException(e, call, methodName, headers);
            return new ServerCall.Listener<ReqT>() {};
        }

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
            @Override
            public void onMessage(ReqT message) {
                try {
                    super.onMessage(message);
                } catch (Exception e) {
                    handleException(e, call, methodName, headers);
                }
            }

            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (Exception e) {
                    handleException(e, call, methodName, headers);
                }
            }

            @Override
            public void onReady() {
                try {
                    super.onReady();
                } catch (Exception e) {
                    handleException(e, call, methodName, headers);
                }
            }

            @Override
            public void onCancel() {
                try {
                    super.onCancel();
                } catch (Exception e) {
                    handleException(e, call, methodName, headers);
                }
            }

            @Override
            public void onComplete() {
                try {
                    super.onComplete();
                } catch (Exception e) {
                    handleException(e, call, methodName, headers);
                }
            }
        };
    }

    private void handleException(Exception e, ServerCall<?, ?> call, String methodName, Metadata headers) {
        Status status;
        Metadata trailers = new Metadata();

        if (e instanceof HttpException httpException) {
            ErrorDefinition error = httpException.getError();
            HttpStatus httpStatus = error.getHttpStatus();
            Status.Code grpcCode = mapHttpStatusToGrpcCode(httpStatus);
            
            status = Status.fromCode(grpcCode).withDescription(error.getMessage()).withCause(e);
            
            trailers.put(ERROR_CODE_KEY, error.getCode());
            trailers.put(ERROR_MESSAGE_KEY, error.getMessage());
            trailers.put(ERROR_STATUS_KEY, String.valueOf(httpStatus.value()));

            try {
                com.google.rpc.Status statusProto = com.google.rpc.Status.newBuilder()
                        .setCode(grpcCode.value())
                        .setMessage(error.getMessage())
                        .addDetails(com.google.protobuf.Any.pack(com.google.rpc.ErrorInfo.newBuilder()
                                .setReason(error.getCode())
                                .setDomain("uniwise")
                                .putMetadata("http_status", String.valueOf(httpStatus.value()))
                                .build()))
                        .build();
                StatusRuntimeException ex = StatusProto.toStatusRuntimeException(statusProto);
                Metadata detailsTrailers = Status.trailersFromThrowable(ex);
                if (detailsTrailers != null) {
                    trailers.merge(detailsTrailers);
                }
            } catch (Exception ex) {
                log.error("Failed to build rich gRPC error details", ex);
            }

            log.warn("gRPC business error [{}]: code={}, status={}, msg={}", 
                     methodName, error.getCode(), httpStatus.value(), error.getMessage());
        } else if (e instanceof AccessDeniedException) {
            status = Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e);
            trailers.put(ERROR_CODE_KEY, "AUTH_ACCESS_DENIED");
            trailers.put(ERROR_MESSAGE_KEY, e.getMessage());
            trailers.put(ERROR_STATUS_KEY, "403");
            log.warn("gRPC access denied [{}]: {}", methodName, e.getMessage());
        } else {
            status = Status.INTERNAL.withDescription("Internal server error").withCause(e);
            trailers.put(ERROR_CODE_KEY, "INTERNAL_SERVER_ERROR");
            trailers.put(ERROR_MESSAGE_KEY, e.getMessage() != null ? e.getMessage() : "An unexpected error occurred");
            trailers.put(ERROR_STATUS_KEY, "500");
            log.error("gRPC unhandled exception [{}] headers={}: ", methodName, filterHeaders(headers), e);
        }

        call.close(status, trailers);
    }

    private Status.Code mapHttpStatusToGrpcCode(HttpStatus httpStatus) {
        if (httpStatus == null) {
            return Status.Code.UNKNOWN;
        }
        return switch (httpStatus) {
            case BAD_REQUEST -> Status.Code.INVALID_ARGUMENT;
            case UNAUTHORIZED -> Status.Code.UNAUTHENTICATED;
            case FORBIDDEN -> Status.Code.PERMISSION_DENIED;
            case NOT_FOUND -> Status.Code.NOT_FOUND;
            case CONFLICT -> Status.Code.ALREADY_EXISTS;
            case TOO_MANY_REQUESTS -> Status.Code.RESOURCE_EXHAUSTED;
            case CONTENT_TOO_LARGE -> Status.Code.OUT_OF_RANGE;
            case NOT_IMPLEMENTED -> Status.Code.UNIMPLEMENTED;
            case SERVICE_UNAVAILABLE -> Status.Code.UNAVAILABLE;
            case GATEWAY_TIMEOUT -> Status.Code.DEADLINE_EXCEEDED;
            default -> {
                if (httpStatus.is4xxClientError()) {
                    yield Status.Code.FAILED_PRECONDITION;
                } else if (httpStatus.is5xxServerError()) {
                    yield Status.Code.INTERNAL;
                }
                yield Status.Code.UNKNOWN;
            }
        };
    }

    private String filterHeaders(Metadata headers) {
        if (headers == null) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String keyName : headers.keys()) {
            if (keyName.equalsIgnoreCase("authorization") || keyName.equalsIgnoreCase("token") || keyName.endsWith("-bin")) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            sb.append(keyName).append("=").append(headers.get(Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER)));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
