package com.uniwise.platform_event_starter.publisher;

import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.uniwise.platform_event_contract.constant.EventHeaders;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ implementation of the EventPublisher interface.
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String applicationName;

    @Override
    public <T> void publish(String routingKey, T payload) {
        publish(Exchanges.EVENTS, routingKey, payload);
    }

    @Override
    public <T> void publish(EventEnvelope<T> envelope, String routingKey) {
        publish(Exchanges.EVENTS, envelope, routingKey);
    }

    @Override
    public <T> void publish(String exchange, String routingKey, T payload) {
        if (payload == null) {
            log.warn("Attempted to publish null payload to exchange: {}, routingKey: {}", exchange, routingKey);
            return;
        }

        EventEnvelope<T> envelope = EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(payload.getClass().getSimpleName())
                .timestamp(Instant.now())
                .producer(applicationName)
                .payload(payload)
                .build();

        publish(exchange, envelope, routingKey);
    }

    @Override
    public <T> void publish(String exchange, EventEnvelope<T> envelope, String routingKey) {
        log.info("Publishing event {} of type {} to exchange {} with routingKey: {}", 
                envelope.getEventId(), envelope.getEventType(), exchange, routingKey);

        rabbitTemplate.convertAndSend(exchange, routingKey, envelope, message -> {
            message.getMessageProperties().setHeader(EventHeaders.CORRELATION_ID, envelope.getEventId());
            message.getMessageProperties().setHeader(EventHeaders.PRODUCER, envelope.getProducer());
            return message;
        });
    }
}
