package com.nsbm.application_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:vacancy.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange vacancyExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue vacancyDeletedQueue() {
        return new Queue("vacancy.deleted.queue", true);
    }

    @Bean
    public Binding vacancyDeletedBinding(Queue vacancyDeletedQueue, TopicExchange vacancyExchange) {
        return BindingBuilder.bind(vacancyDeletedQueue).to(vacancyExchange).with("vacancy.deleted");
    }

    public static final String APPLICATION_AI_QUEUE = "application.ai.match.queue";
    public static final String APPLICATION_SUBMITTED_ROUTING_KEY = "application.submitted.match";

    @Bean
    public Queue applicationAiQueue() {
        return new Queue(APPLICATION_AI_QUEUE, true);
    }

    @Bean
    public Binding applicationAiBinding(Queue applicationAiQueue, TopicExchange vacancyExchange) {
        return BindingBuilder.bind(applicationAiQueue).to(vacancyExchange).with(APPLICATION_SUBMITTED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
