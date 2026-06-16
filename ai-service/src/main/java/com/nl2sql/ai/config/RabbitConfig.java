package com.nl2sql.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NL2SQL_QUEUE = "nl2sql.queue";
    public static final String NL2SQL_EXCHANGE = "nl2sql.exchange";
    public static final String NL2SQL_ROUTING_KEY = "nl2sql.event";

    @Bean
    public Queue nl2sqlQueue() {
        return new Queue(NL2SQL_QUEUE, true);
    }

    @Bean
    public TopicExchange nl2sqlExchange() {
        return new TopicExchange(NL2SQL_EXCHANGE);
    }

    @Bean
    public Binding nl2sqlBinding(Queue nl2sqlQueue, TopicExchange nl2sqlExchange) {
        return BindingBuilder.bind(nl2sqlQueue).to(nl2sqlExchange).with(NL2SQL_ROUTING_KEY);
    }
}
