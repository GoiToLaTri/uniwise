package com.uniwise.ffmpeg_worker.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.uniwise.platform_event_contract.constant.RoutingKeys;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_UPLOADED_QUEUE = "ffmpeg.media.video-uploaded.queue";

    @Bean
    public Queue videoUploadedQueue() {
        return new Queue(VIDEO_UPLOADED_QUEUE, true); // durable
    }

    @Bean
    public Binding videoUploadedBinding(Queue videoUploadedQueue, TopicExchange mediaExchange) {
        return BindingBuilder.bind(videoUploadedQueue)
                .to(mediaExchange)
                .with(RoutingKeys.VIDEO_UPLOADED);
    }
}
