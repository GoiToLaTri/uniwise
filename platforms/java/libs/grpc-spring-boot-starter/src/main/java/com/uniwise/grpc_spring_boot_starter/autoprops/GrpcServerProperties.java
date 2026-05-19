package com.uniwise.grpc_spring_boot_starter.autoprops;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "grpc.server")
public class GrpcServerProperties {
    /**
     * Port của gRPC Server. Mặc định là 9090.
     */
    private int port = 9090;
}
