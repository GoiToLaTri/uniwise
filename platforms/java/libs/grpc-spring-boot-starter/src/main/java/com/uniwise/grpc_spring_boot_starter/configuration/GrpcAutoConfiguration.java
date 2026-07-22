package com.uniwise.grpc_spring_boot_starter.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.uniwise.grpc_spring_boot_starter.autoprops.GrpcClientProperties;
import com.uniwise.grpc_spring_boot_starter.autoprops.GrpcServerProperties;
import com.uniwise.grpc_spring_boot_starter.client.GrpcClientBeanPostProcessor;
import com.uniwise.grpc_spring_boot_starter.interceptor.ExceptionHandlingInterceptor;
import com.uniwise.grpc_spring_boot_starter.interceptor.LoggingInterceptor;
import com.uniwise.grpc_spring_boot_starter.server.GrpcServerRunner;

@AutoConfiguration
@EnableConfigurationProperties({
        GrpcServerProperties.class,
        GrpcClientProperties.class
})
public class GrpcAutoConfiguration {
    public static final String GRPC_SERVER_EXECUTOR_BEAN_NAME = "grpcServerExecutor";

    @Bean(name = GRPC_SERVER_EXECUTOR_BEAN_NAME, destroyMethod = "shutdownNow")
    @ConditionalOnThreading(Threading.VIRTUAL)
    @ConditionalOnBooleanProperty(prefix = "grpc.server.virtual-threads", name = "enabled", matchIfMissing = true)
    @ConditionalOnMissingBean(name = GRPC_SERVER_EXECUTOR_BEAN_NAME)
    public ExecutorService grpcServerExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcServerRunner grpcServerRunner(
            ApplicationContext applicationContext,
            GrpcServerProperties properties,
            @Qualifier(GRPC_SERVER_EXECUTOR_BEAN_NAME) ObjectProvider<ExecutorService> executorProvider) {
        return new GrpcServerRunner(
                applicationContext,
                properties.getPort(),
                properties.getShutdownGracePeriod(),
                executorProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcClientBeanPostProcessor grpcClientBeanPostProcessor(GrpcClientProperties properties) {
        return new GrpcClientBeanPostProcessor(properties);
    }

    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    @Bean
    public ExceptionHandlingInterceptor exceptionHandlingInterceptor() {
        return new ExceptionHandlingInterceptor();
    }
}
