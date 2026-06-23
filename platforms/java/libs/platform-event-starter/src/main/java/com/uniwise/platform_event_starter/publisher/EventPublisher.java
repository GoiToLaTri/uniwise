package com.uniwise.platform_event_starter.publisher;

import com.uniwise.platform_event_contract.envelope.EventEnvelope;

/**
 * Interface representing the event publishing component.
 */
public interface EventPublisher {
    
    /**
     * Publishes a raw payload by wrapping it in an EventEnvelope automatically.
     *
     * @param routingKey the routing key
     * @param payload the event payload to publish
     * @param <T> the payload type
     */
    <T> void publish(String routingKey, T payload);

    /**
     * Publishes a pre-constructed EventEnvelope.
     *
     * @param envelope the wrapped event envelope
     * @param routingKey the routing key
     * @param <T> the payload type
     */
    <T> void publish(EventEnvelope<T> envelope, String routingKey);

    /**
     * Publishes a raw payload by wrapping it in an EventEnvelope automatically, using a custom exchange.
     *
     * @param exchange the target exchange
     * @param routingKey the routing key
     * @param payload the event payload to publish
     * @param <T> the payload type
     */
    <T> void publish(String exchange, String routingKey, T payload);

    /**
     * Publishes a pre-constructed EventEnvelope to a custom exchange.
     *
     * @param exchange the target exchange
     * @param envelope the wrapped event envelope
     * @param routingKey the routing key
     * @param <T> the payload type
     */
    <T> void publish(String exchange, EventEnvelope<T> envelope, String routingKey);
}
