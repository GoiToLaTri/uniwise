package com.uniwise.grpc_spring_boot_starter.client;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;
import com.uniwise.grpc_spring_boot_starter.autoprops.GrpcClientProperties;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcClientBeanPostProcessor implements BeanPostProcessor, DisposableBean {
    private final GrpcClientProperties clientProperties;
    final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();

    public GrpcClientBeanPostProcessor(GrpcClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    /**
     * Post-process bean before initialization.
     * 
     * <p>
     * Dùng để inject gRPC client vào các field hoặc method.
     * 
     * @param bean     Bean đang được xử lý
     * @param beanName Tên của bean
     * @return Bean đã được xử lý
     * @throws BeansException Nếu có lỗi xảy ra
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            GrpcClient annotation = field.getAnnotation(GrpcClient.class);
            if (annotation != null) {
                String clientName = annotation.value();
                ManagedChannel channel = getOrCreateChannel(clientName);

                // Dùng reflection để tạo Stub từ class của field (ví dụ: MyServiceBlockingStub)
                Object stub = createStub(field.getType(), channel);

                ReflectionUtils.makeAccessible(field);
                field.set(bean, stub);
            }
        });
        return bean;
    }

    private ManagedChannel getOrCreateChannel(String name) {
        return channelCache.computeIfAbsent(name, key -> {
            var config = clientProperties.getClients().getOrDefault(key, new GrpcClientProperties.ClientChannel());
            return ManagedChannelBuilder.forAddress(config.getHost(), config.getPort())
                    .usePlaintext()
                    .build();
        });
    }

    /**
     * Tạo stub cho client.
     * 
     * <p>
     * Dùng để tạo stub cho client.
     * 
     * @param stubClass Class của stub
     * @param channel   Channel cho client
     * @return Stub cho client
     * @throws RuntimeException Nếu có lỗi xảy ra
     */
    private Object createStub(Class<?> stubClass, ManagedChannel channel) {
        try {
            // Tìm method static 'newBlockingStub', 'newStub', hoặc 'newFutureStub' trong
            // class cha (ServiceGrpc)
            Class<?> enclosingClass = stubClass.getEnclosingClass();
            String simpleName = stubClass.getSimpleName();
            String methodName;
            if (simpleName.endsWith("BlockingStub")) {
                methodName = "newBlockingStub";
            } else if (simpleName.endsWith("FutureStub")) {
                methodName = "newFutureStub";
            } else if (simpleName.endsWith("Stub")) {
                methodName = "newStub";
            } else {
                methodName = "newBlockingStub";
            }

            Method factoryMethod = enclosingClass.getMethod(methodName, io.grpc.Channel.class);
            return factoryMethod.invoke(null, channel);
        } catch (Exception e) {
            throw new RuntimeException("Could not create gRPC stub for " + stubClass.getName(), e);
        }
    }

    @Override
    public void destroy() {
        boolean interrupted = false;
        try {
            for (Map.Entry<String, ManagedChannel> entry : channelCache.entrySet()) {
                ManagedChannel channel = entry.getValue();
                if (channel != null && !channel.isShutdown()) {
                    try {
                        channel.shutdown();
                    } catch (RuntimeException e) {
                        log.warn("Error initiating shutdown for gRPC channel '{}'", entry.getKey(), e);
                    }
                }
            }

            long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            for (Map.Entry<String, ManagedChannel> entry : channelCache.entrySet()) {
                ManagedChannel channel = entry.getValue();
                if (channel == null || channel.isTerminated()) {
                    continue;
                }
                try {
                    long remainingNanos = deadlineNanos - System.nanoTime();
                    if (interrupted || remainingNanos <= 0) {
                        channel.shutdownNow();
                        continue;
                    }
                    if (!channel.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS)) {
                        log.warn("gRPC channel '{}' did not terminate within 5s, forcing shutdownNow()",
                                entry.getKey());
                        channel.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                    log.warn("Interrupted while shutting down gRPC channel '{}', forcing shutdownNow()",
                            entry.getKey(), e);
                    try {
                        channel.shutdownNow();
                    } catch (RuntimeException ex) {
                        log.warn("Error forcing shutdownNow for gRPC channel '{}'", entry.getKey(), ex);
                    }
                } catch (RuntimeException e) {
                    log.warn("Error awaiting termination for gRPC channel '{}'", entry.getKey(), e);
                }
            }
        } finally {
            channelCache.clear();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
