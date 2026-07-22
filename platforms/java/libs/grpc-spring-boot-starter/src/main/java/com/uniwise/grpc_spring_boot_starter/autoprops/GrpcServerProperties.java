package com.uniwise.grpc_spring_boot_starter.autoprops;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "grpc.server")
public class GrpcServerProperties {
    /**
     * Port của gRPC Server. Mặc định là 9090.
     */
    private int port = 9090;

    /**
     * Maximum time to wait for in-flight calls before forcing server shutdown.
     */
    private Duration shutdownGracePeriod = Duration.ofSeconds(30);

    /**
     * Virtual-thread settings for inbound gRPC callbacks.
     */
    private final VirtualThreads virtualThreads = new VirtualThreads();

    @Data
    public static class VirtualThreads {
        /**
         * Use a virtual thread per inbound callback when Spring virtual threads are enabled.
         */
        private boolean enabled = true;
    }
}
