package com.uniwise.grpc_spring_boot_starter.annotation;

import org.springframework.stereotype.Service;
import java.lang.annotation.*;

/**
 * Dùng để đánh dấu các class triển khai logic gRPC
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service // Để Spring quản lý Bean này luôn
public @interface GrpcService {
}