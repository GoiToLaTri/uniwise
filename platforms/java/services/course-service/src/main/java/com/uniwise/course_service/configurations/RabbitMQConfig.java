package com.uniwise.course_service.configurations;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uniwise.platform_event_contract.constant.RoutingKeys;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_TRANSCODED_QUEUE = "course.video-transcoded.queue";
    public static final String VIDEO_UPLOADED_QUEUE = "course.video-uploaded.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "course.payment-completed.queue";

    @Bean
    public Queue videoTranscodedQueue() {
        return new Queue(VIDEO_TRANSCODED_QUEUE, true); // durable
    }

    @Bean
    public Queue videoUploadedQueue() {
        return new Queue(VIDEO_UPLOADED_QUEUE, true); // durable
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return new Queue(PAYMENT_COMPLETED_QUEUE, true); // durable
    }

    @Bean
    public Binding videoTranscodedBinding(Queue videoTranscodedQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(videoTranscodedQueue)
                .to(mediaExchange)
                .with(RoutingKeys.VIDEO_TRANSCODED);
    }

    @Bean
    public Binding videoUploadedBinding(Queue videoUploadedQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(videoUploadedQueue)
                .to(mediaExchange)
                .with(RoutingKeys.VIDEO_UPLOADED);
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange platformExchange) {
        return BindingBuilder.bind(paymentCompletedQueue)
                .to(platformExchange)
                .with(RoutingKeys.PAYMENT_COMPLETED);
    }
}

