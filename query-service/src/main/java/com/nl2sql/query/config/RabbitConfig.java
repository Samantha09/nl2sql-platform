package com.nl2sql.query.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NL2SQL_EXCHANGE = "nl2sql.exchange";
    public static final String NL2SQL_ROUTING_KEY = "nl2sql.event";

    public static final String SQL_READY_QUEUE = "sql.ready.queue";
    public static final String SQL_READY_EXCHANGE = "sql.ready.exchange";
    public static final String SQL_READY_ROUTING_KEY = "sql.ready.event";

    public static final String RESULT_READY_QUEUE = "result.ready.queue";
    public static final String RESULT_READY_EXCHANGE = "result.ready.exchange";
    public static final String RESULT_READY_ROUTING_KEY = "result.ready.event";

    @Bean
    public TopicExchange nl2sqlExchange() {
        return new TopicExchange(NL2SQL_EXCHANGE);
    }

    @Bean
    public Queue sqlReadyQueue() {
        return new Queue(SQL_READY_QUEUE, true);
    }

    @Bean
    public DirectExchange sqlReadyExchange() {
        return new DirectExchange(SQL_READY_EXCHANGE);
    }

    @Bean
    public Binding sqlReadyBinding(Queue sqlReadyQueue, DirectExchange sqlReadyExchange) {
        return BindingBuilder.bind(sqlReadyQueue).to(sqlReadyExchange).with(SQL_READY_ROUTING_KEY);
    }

    @Bean
    public Queue resultReadyQueue() {
        return new Queue(RESULT_READY_QUEUE, true);
    }

    @Bean
    public DirectExchange resultReadyExchange() {
        return new DirectExchange(RESULT_READY_EXCHANGE);
    }

    @Bean
    public Binding resultReadyBinding(Queue resultReadyQueue, DirectExchange resultReadyExchange) {
        return BindingBuilder.bind(resultReadyQueue).to(resultReadyExchange).with(RESULT_READY_ROUTING_KEY);
    }
}
