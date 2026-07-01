package com.uniwise.course_service.configuration;

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

    @Bean
    public Queue videoTranscodedQueue() {
        return new Queue(VIDEO_TRANSCODED_QUEUE, true); // durable
    }

    @Bean
    public Binding videoTranscodedBinding(Queue videoTranscodedQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(videoTranscodedQueue)
                .to(mediaExchange)
                .with(RoutingKeys.VIDEO_TRANSCODED);
    }
}
