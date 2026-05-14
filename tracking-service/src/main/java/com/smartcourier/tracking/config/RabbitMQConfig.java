package com.smartcourier.tracking.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "tracking.queue";
    public static final String EXCHANGE = "delivery.exchange";
    public static final String ROUTING_KEY = "delivery.status";

    @Bean
    public Queue trackingQueue() {
        return new Queue(QUEUE);
    } //broker

    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue trackingQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(trackingQueue).to(deliveryExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
