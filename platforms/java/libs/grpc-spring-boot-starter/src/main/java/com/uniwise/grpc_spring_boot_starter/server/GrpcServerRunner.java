package com.uniwise.grpc_spring_boot_starter.server;

import java.io.IOException;
import java.util.Map;

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
    private Server server; // Instance của gRPC server
    private boolean isRunning = false; // Trạng thái hoạt động của server

    /**
     * Constructor của GrpcServerRunner.
     * 
     * @param applicationContext Spring context để truy xuất các bean
     * @param port               Port lắng nghe của gRPC server
     */
    public GrpcServerRunner(ApplicationContext applicationContext, int port) {
        this.applicationContext = applicationContext;
        this.port = port;
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

            // Chạy một Thread daemon để tránh block main thread của Spring Boot
            Thread awaitThread = new Thread(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    log.error("gRPC server awaited termination interrupted");
                }
            });
            awaitThread.setDaemon(false);
            awaitThread.start();

        } catch (IOException e) {
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
        if (server != null) {
            log.info("Shutting down gRPC server...");
            server.shutdown();
            isRunning = false;
            log.info("gRPC server stopped.");
        }
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
