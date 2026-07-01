package com.uniwise.media_service.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uniwise.platform_event_contract.constant.RoutingKeys;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_PROCESSED_QUEUE = "media.video-processed.queue";

    @Bean
    public Queue videoProcessedQueue() {
        return new Queue(VIDEO_PROCESSED_QUEUE, true); // durable
    }

    @Bean
    public Binding videoProcessedBinding(Queue videoProcessedQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(videoProcessedQueue)
                .to(mediaExchange)
                .with(RoutingKeys.VIDEO_PROCESSED);
    }
}
