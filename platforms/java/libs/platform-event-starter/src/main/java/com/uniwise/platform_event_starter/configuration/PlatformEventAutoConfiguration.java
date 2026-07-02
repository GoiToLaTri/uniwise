package com.uniwise.platform_event_starter.configuration;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_starter.publisher.EventPublisher;
import com.uniwise.platform_event_starter.publisher.RabbitEventPublisher;

/**
 * Auto-configuration for platform-level messaging and events.
 */
@AutoConfiguration
public class PlatformEventAutoConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter messageConverter() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate) {
        return new RabbitEventPublisher(rabbitTemplate, applicationName);
    }

    @Bean
    @ConditionalOnMissingBean(name = "platformExchange")
    public TopicExchange platformExchange() {

        return new TopicExchange(Exchanges.EVENTS);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mediaExchange")
    public TopicExchange mediaExchange() {
        return new TopicExchange(Exchanges.MEDIA);
    }
}
