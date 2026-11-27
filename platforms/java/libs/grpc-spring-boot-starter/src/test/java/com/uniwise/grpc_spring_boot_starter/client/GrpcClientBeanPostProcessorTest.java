package com.uniwise.grpc_spring_boot_starter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;
import com.uniwise.grpc_spring_boot_starter.autoprops.GrpcClientProperties;
import com.uniwise.grpc_spring_boot_starter.interceptor.ExceptionHandlingInterceptor;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;

class GrpcClientBeanPostProcessorTest {

    public static class FakeServiceGrpc {
        public static FakeServiceBlockingStub newBlockingStub(Channel channel) {
            return new FakeServiceBlockingStub(channel);
        }

        public static FakeServiceFutureStub newFutureStub(Channel channel) {
            return new FakeServiceFutureStub(channel);
        }

        public static FakeServiceStub newStub(Channel channel) {
            return new FakeServiceStub(channel);
        }

        public static class FakeServiceBlockingStub {
            final Channel channel;

            FakeServiceBlockingStub(Channel channel) {
                this.channel = channel;
            }
        }

        public static class FakeServiceFutureStub {
            final Channel channel;

            FakeServiceFutureStub(Channel channel) {
                this.channel = channel;
            }
        }

        public static class FakeServiceStub {
            final Channel channel;

            FakeServiceStub(Channel channel) {
                this.channel = channel;
            }
        }
    }

    static class TargetClientBean {
        @GrpcClient("user-service")
        FakeServiceGrpc.FakeServiceBlockingStub blockingStub;

        @GrpcClient("user-service")
        FakeServiceGrpc.FakeServiceFutureStub futureStub;

        @GrpcClient("course-service")
        FakeServiceGrpc.FakeServiceStub asyncStub;
    }

    @Test
    void injectsBlockingFutureAndAsyncStubsCorrectly() {
        GrpcClientProperties properties = new GrpcClientProperties();
        GrpcClientBeanPostProcessor processor = new GrpcClientBeanPostProcessor(properties);
        TargetClientBean bean = new TargetClientBean();

        try {
            processor.postProcessBeforeInitialization(bean, "targetClientBean");

            assertNotNull(bean.blockingStub);
            assertNotNull(bean.futureStub);
            assertNotNull(bean.asyncStub);
            assertSame(bean.blockingStub.channel, bean.futureStub.channel);
            assertEquals(2, processor.channelCache.size());
        } finally {
            processor.destroy();
        }
    }

    @Test
    void destroyShutsDownAllManagedChannelsAndClearsCache() {
        GrpcClientProperties properties = new GrpcClientProperties();
        GrpcClientBeanPostProcessor processor = new GrpcClientBeanPostProcessor(properties);
        TargetClientBean bean = new TargetClientBean();

        processor.postProcessBeforeInitialization(bean, "targetClientBean");
        ManagedChannel userChannel = processor.channelCache.get("user-service");
        ManagedChannel courseChannel = processor.channelCache.get("course-service");

        assertNotNull(userChannel);
        assertNotNull(courseChannel);
        assertFalse(userChannel.isShutdown());
        assertFalse(courseChannel.isShutdown());

        processor.destroy();

        assertTrue(userChannel.isShutdown());
        assertTrue(courseChannel.isShutdown());
        assertTrue(processor.channelCache.isEmpty());
    }

    @Test
    void destroyForcesShutdownNowWhenChannelDoesNotTerminateInTime() throws InterruptedException {
        GrpcClientBeanPostProcessor processor = new GrpcClientBeanPostProcessor(new GrpcClientProperties());
        ManagedChannel hangingChannel = mock(ManagedChannel.class);
        ManagedChannel cleanChannel = mock(ManagedChannel.class);

        when(hangingChannel.isShutdown()).thenReturn(false);
        when(hangingChannel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);
        when(cleanChannel.isShutdown()).thenReturn(false);
        when(cleanChannel.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

        processor.channelCache.put("hanging", hangingChannel);
        processor.channelCache.put("clean", cleanChannel);

        processor.destroy();

        verify(hangingChannel).shutdown();
        verify(hangingChannel).awaitTermination(5, TimeUnit.SECONDS);
        verify(hangingChannel).shutdownNow();

        verify(cleanChannel).shutdown();
        verify(cleanChannel).awaitTermination(5, TimeUnit.SECONDS);
        verify(cleanChannel, never()).shutdownNow();

        assertTrue(processor.channelCache.isEmpty());
    }

    @Test
    void destroyRestoresInterruptFlagAndForcesShutdownNowOnInterrupt() throws InterruptedException {
        GrpcClientBeanPostProcessor processor = new GrpcClientBeanPostProcessor(new GrpcClientProperties());
        ManagedChannel channel = mock(ManagedChannel.class);
        when(channel.isShutdown()).thenReturn(false);
        when(channel.awaitTermination(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("stop"));

        processor.channelCache.put("interrupted", channel);

        try {
            processor.destroy();
            assertTrue(Thread.currentThread().isInterrupted());
            verify(channel).shutdown();
            verify(channel).shutdownNow();
            assertTrue(processor.channelCache.isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void exceptionHandlingInterceptorHandlesAccessDeniedWithNullMessage() {
        ExceptionHandlingInterceptor interceptor = new ExceptionHandlingInterceptor();
        ServerCall<String, String> call = mock(ServerCall.class);
        MethodDescriptor<String, String> descriptor = mock(MethodDescriptor.class);
        when(call.getMethodDescriptor()).thenReturn(descriptor);
        when(descriptor.getFullMethodName()).thenReturn("uniwise.TestService/SecureCall");

        ServerCallHandler<String, String> handler = (c, h) -> {
            throw new AccessDeniedException(null);
        };

        interceptor.interceptCall(call, new Metadata(), handler);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<Metadata> trailersCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(call).close(statusCaptor.capture(), trailersCaptor.capture());

        assertEquals(Status.Code.PERMISSION_DENIED, statusCaptor.getValue().getCode());
        assertEquals("Access denied", statusCaptor.getValue().getDescription());
        assertEquals("AUTH_ACCESS_DENIED", trailersCaptor.getValue().get(ExceptionHandlingInterceptor.ERROR_CODE_KEY));
        assertEquals("Access denied", trailersCaptor.getValue().get(ExceptionHandlingInterceptor.ERROR_MESSAGE_KEY));
        assertEquals("403", trailersCaptor.getValue().get(ExceptionHandlingInterceptor.ERROR_STATUS_KEY));
    }

    @Test
    @SuppressWarnings("unchecked")
    void exceptionHandlingInterceptorDoesNotLeakInternalErrorMessage() {
        ExceptionHandlingInterceptor interceptor = new ExceptionHandlingInterceptor();
        ServerCall<String, String> call = mock(ServerCall.class);
        MethodDescriptor<String, String> descriptor = mock(MethodDescriptor.class);
        when(call.getMethodDescriptor()).thenReturn(descriptor);
        when(descriptor.getFullMethodName()).thenReturn("uniwise.TestService/BrokenCall");

        ServerCallHandler<String, String> handler = (c, h) -> {
            throw new RuntimeException("SQL syntax error near secret_table");
        };

        interceptor.interceptCall(call, new Metadata(), handler);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<Metadata> trailersCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(call).close(statusCaptor.capture(), trailersCaptor.capture());

        assertEquals(Status.Code.INTERNAL, statusCaptor.getValue().getCode());
        assertEquals("Internal server error", statusCaptor.getValue().getDescription());
        assertEquals("INTERNAL_SERVER_ERROR", trailersCaptor.getValue().get(ExceptionHandlingInterceptor.ERROR_CODE_KEY));
        assertEquals("An unexpected error occurred",
                trailersCaptor.getValue().get(ExceptionHandlingInterceptor.ERROR_MESSAGE_KEY));
        assertEquals("500", trailersCaptor.getValue().get(ExceptionHandlingInterceptor.ERROR_STATUS_KEY));
    }

    @Test
    void exceptionHandlingInterceptorFiltersSensitiveHeadersIncludingXAuthToken() {
        ExceptionHandlingInterceptor interceptor = new ExceptionHandlingInterceptor();
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer secret-jwt");
        headers.put(Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER), "raw-token");
        headers.put(Metadata.Key.of("x-auth-token", Metadata.ASCII_STRING_MARSHALLER), "secret-auth-token");
        headers.put(Metadata.Key.of("trace-bin", Metadata.BINARY_BYTE_MARSHALLER), new byte[] { 1, 2, 3 });
        headers.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");

        String filtered = ReflectionTestUtils.invokeMethod(interceptor, "filterHeaders", headers);

        assertNotNull(filtered);
        assertTrue(filtered.contains("x-request-id=req-123"));
        assertFalse(filtered.contains("authorization"));
        assertFalse(filtered.contains("secret-jwt"));
        assertFalse(filtered.contains("raw-token"));
        assertFalse(filtered.contains("x-auth-token"));
        assertFalse(filtered.contains("secret-auth-token"));
        assertFalse(filtered.contains("trace-bin"));
    }
}
