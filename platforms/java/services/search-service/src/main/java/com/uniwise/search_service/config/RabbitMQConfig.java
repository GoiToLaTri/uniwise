package com.uniwise.search_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;

@Configuration
public class RabbitMQConfig {

    // --- Course Lifecycle Events ---
    public static final String COURSE_CREATED_QUEUE = "search.course.created";
    public static final String COURSE_UPDATED_QUEUE = "search.course.updated";
    public static final String COURSE_DELETED_QUEUE = "search.course.deleted";

    public static final String COURSE_METRICS_SYNC_QUEUE = "search.course.metrics.sync";
    public static final String COURSE_METRICS_SYNC_DLQ = "search.course.metrics.sync.dlq";

    @Bean
    public Queue courseMetricsSyncDlq() {
        return new Queue(COURSE_METRICS_SYNC_DLQ, true);
    }

    @Bean
    public Binding courseMetricsSyncDlqBinding(Queue courseMetricsSyncDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(courseMetricsSyncDlq)
                .to(dlxExchange)
                .with(RoutingKeys.COURSE_METRICS_SYNC); // Hoặc bắt kỳ DLQ key nào
    }

    @Bean
    public Queue courseMetricsSyncQueue() {
        return QueueBuilder.durable(COURSE_METRICS_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", Exchanges.DLX)
                .withArgument("x-dead-letter-routing-key", RoutingKeys.COURSE_METRICS_SYNC)
                .build();
    }

    @Bean
    public Binding courseMetricsSyncBinding(Queue courseMetricsSyncQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(courseMetricsSyncQueue)
                .to(platformExchange)
                .with(RoutingKeys.COURSE_METRICS_SYNC);
    }

    @Bean
    public Queue courseCreatedQueue() {
        return new Queue(COURSE_CREATED_QUEUE, true);
    }

    @Bean
    public Binding courseCreatedBinding(Queue courseCreatedQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(courseCreatedQueue)
                .to(platformExchange)
                .with(RoutingKeys.COURSE_CREATED);
    }

    @Bean
    public Queue courseUpdatedQueue() {
        return new Queue(COURSE_UPDATED_QUEUE, true);
    }

    @Bean
    public Binding courseUpdatedBinding(Queue courseUpdatedQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(courseUpdatedQueue)
                .to(platformExchange)
                .with(RoutingKeys.COURSE_UPDATED);
    }

    @Bean
    public Queue courseDeletedQueue() {
        return new Queue(COURSE_DELETED_QUEUE, true);
    }

    @Bean
    public Binding courseDeletedBinding(Queue courseDeletedQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(courseDeletedQueue)
                .to(platformExchange)
                .with(RoutingKeys.COURSE_DELETED);
    }

    // Bean TopicExchange platformExchange và dlxExchange đã được cấu hình tự động trong PlatformEventAutoConfiguration 
    // của platform-event-starter thông qua @ConditionalOnMissingBean
}
