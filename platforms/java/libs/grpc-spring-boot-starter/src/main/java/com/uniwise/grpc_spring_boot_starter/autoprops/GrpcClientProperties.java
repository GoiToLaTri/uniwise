package com.uniwise.grpc_spring_boot_starter.autoprops;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Cấu hình cho gRPC client.
 * 
 * <p>
 * Annotation này sẽ được sử dụng để inject gRPC client vào các field hoặc
 * method.
 * 
 * <p>
 * Cơ chế hoạt động:
 * <ul>
 * <li>Tìm tất cả các bean có annotation @GrpcClient</li>
 * <li>Tạo gRPC client và inject vào các field hoặc method</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "grpc")
public class GrpcClientProperties {
    // Map chứa cấu hình cho nhiều client khác nhau
    // key là tên service (trùng với value trong @GrpcClient)
    private Map<String, ClientChannel> clients = new HashMap<>();

    @Data
    public static class ClientChannel {
        private String host = "127.0.0.1";
        private int port = 9090;
    }
}
