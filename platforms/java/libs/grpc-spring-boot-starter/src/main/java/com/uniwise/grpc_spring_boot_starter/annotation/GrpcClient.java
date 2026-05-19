package com.uniwise.grpc_spring_boot_starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dùng để đánh dấu các field hoặc method là của gRPC client.
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
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcClient {
    String value(); // Tên của service muốn gọi (ví dụ: "inventory-service")
}