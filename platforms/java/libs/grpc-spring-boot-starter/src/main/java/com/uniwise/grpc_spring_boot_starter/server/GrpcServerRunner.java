package com.uniwise.grpc_spring_boot_starter.server;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import com.uniwise.grpc_spring_boot_starter.annotation.GrpcService;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * Class này sẽ tìm tất cả các Bean có gắn `@GrpcService` và khởi chạy server.
 * 
 * <p>
 * Class implements {@link SmartLifecycle} để tự động khởi động và dừng
 * gRPC server theo vòng đời của Spring ApplicationContext.
 * 
 * <p>
 * Cơ chế hoạt động:
 * <ul>
 * <li>Quét tất cả các bean trong Spring context có annotation
 * {@code @GrpcService}</li>
 * <li>Đăng ký các service đó vào gRPC server builder</li>
 * <li>Khởi động server trên port được chỉ định</li>
 * <li>Tạo thread daemon để duy trì server mà không block main thread</li>
 * </ul>
 */
@Slf4j
public class GrpcServerRunner implements SmartLifecycle {

    private final ApplicationContext applicationContext; // Spring context để truy xuất các bean
    private final int port; // Port lắng nghe của gRPC server
    private final Duration shutdownGracePeriod;
    private final Executor executor;
    private Server server; // Instance của gRPC server
    private volatile boolean isRunning = false; // Trạng thái hoạt động của server

    /**
     * Constructor của GrpcServerRunner.
     * 
     * @param applicationContext Spring context để truy xuất các bean
     * @param port               Port lắng nghe của gRPC server
     */
    public GrpcServerRunner(ApplicationContext applicationContext, int port) {
        this(applicationContext, port, Duration.ofSeconds(30), null);
    }

    /**
     * Constructor supporting an optional application executor managed by Spring.
     *
     * @param applicationContext  Spring context để truy xuất các bean
     * @param port                Port lắng nghe của gRPC server
     * @param shutdownGracePeriod Thời gian chờ các RPC đang xử lý hoàn tất
     * @param executor            Executor cho callback gRPC; {@code null} giữ mặc định của gRPC
     */
    public GrpcServerRunner(
            ApplicationContext applicationContext,
            int port,
            Duration shutdownGracePeriod,
            Executor executor) {
        this.applicationContext = applicationContext;
        this.port = port;
        this.shutdownGracePeriod = shutdownGracePeriod;
        this.executor = executor;
    }

    /**
     * Khởi động gRPC server.
     * 
     * <p>
     * Các bước thực hiện:
     * <ol>
     * <li>Tạo ServerBuilder với port được cấu hình</li>
     * <li>Tìm tất cả bean có annotation @GrpcService</li>
     * <li>Đăng ký các service implement BindableService vào server</li>
     * <li>Build và start server</li>
     * <li>Tạo thread riêng để chờ server kết thúc (không block main thread)</li>
     * </ol>
     * 
     * @throws RuntimeException nếu không thể khởi động server (IOException)
     */
    @Override
    public void start() {
        if (port <= 0) {
            log.info("gRPC server is disabled (port = {}). Skipping server startup.", port);
            isRunning = false;
            return;
        }

        log.info("Starting gRPC Server on port: {}", port);
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);

        if (executor != null) {
            serverBuilder.executor(executor);
            log.info("Configured gRPC Server with the Spring-managed executor.");
        }

        // Thêm interceptors vào server builder
        applicationContext.getBeansOfType(ServerInterceptor.class).values().forEach(serverBuilder::intercept);

        // Tìm các Bean có đánh dấu @GrpcService
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(GrpcService.class);

        for (Object serviceBean : beansWithAnnotation.values()) {
            if (serviceBean instanceof BindableService bindableService) {
                serverBuilder.addService(bindableService);
                log.info("Registered gRPC service: {}", serviceBean.getClass().getName());
            } else {
                log.warn("Bean {} has @GrpcService but does not implement BindableService",
                        serviceBean.getClass().getName());
            }
        }

        server = serverBuilder.build();
        try {
            server.start();
            isRunning = true;
            log.info("gRPC Server started successfully.");

            Server startedServer = server;
            // Preserve the existing process-liveness behavior without consuming the RPC executor.
            Thread awaitThread = Thread.ofPlatform()
                    .name("grpc-server-await-termination-" + port)
                    .daemon(false)
                    .unstarted(() -> {
                try {
                    startedServer.awaitTermination();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("gRPC server await-termination thread was interrupted");
                }
            });
            awaitThread.start();

        } catch (IOException e) {
            server.shutdownNow();
            server = null;
            isRunning = false;
            log.error("Failed to start gRPC server", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Dừng gRPC server một cách graceful.
     * 
     * <p>
     * Phương thức này sẽ được gọi tự động khi Spring context shutdown.
     */
    @Override
    public void stop() {
        Server serverToStop = server;
        if (serverToStop == null) {
            isRunning = false;
            return;
        }

        log.info("Shutting down gRPC server...");
        serverToStop.shutdown();

        try {
            long gracePeriodMillis = Math.max(0L, shutdownGracePeriod.toMillis());
            if (!serverToStop.awaitTermination(gracePeriodMillis, TimeUnit.MILLISECONDS)) {
                log.warn("gRPC server did not stop within {}. Forcing shutdown.", shutdownGracePeriod);
                serverToStop.shutdownNow();
                if (!serverToStop.awaitTermination(gracePeriodMillis, TimeUnit.MILLISECONDS)) {
                    log.warn("gRPC server still has active calls after forced shutdown.");
                }
            }
        } catch (InterruptedException e) {
            serverToStop.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for gRPC server shutdown.");
        } finally {
            server = null;
            isRunning = false;
        }

        log.info("gRPC server stopped.");
    }

    /**
     * Kiểm tra trạng thái hoạt động của server.
     * 
     * @return true nếu server đang chạy, false nếu ngược lại
     */
    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
