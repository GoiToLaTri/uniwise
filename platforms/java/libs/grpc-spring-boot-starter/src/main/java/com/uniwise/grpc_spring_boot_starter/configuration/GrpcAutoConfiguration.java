package com.uniwise.grpc_spring_boot_starter.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
    @Bean
    @ConditionalOnMissingBean
    public GrpcServerRunner grpcServerRunner(ApplicationContext applicationContext, GrpcServerProperties properties) {
        return new GrpcServerRunner(applicationContext, properties.getPort());
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
