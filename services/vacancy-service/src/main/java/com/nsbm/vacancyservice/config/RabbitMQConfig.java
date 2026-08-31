package com.nsbm.vacancyservice.config;

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

    public static final String VACANCY_AI_QUEUE = "vacancy.ai.queue";
    public static final String VACANCY_FLYER_ROUTING_KEY = "vacancy.flyer.process";

    @Value("${app.rabbitmq.exchange:vacancy.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.notification.exchange:notification.exchange}")
    private String notificationExchangeName;

    @Bean
    public TopicExchange vacancyExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(notificationExchangeName, true, false);
    }

    @Bean
    public Queue vacancyAiQueue() {
        return new Queue(VACANCY_AI_QUEUE, true);
    }

    @Bean
    public Binding vacancyAiBinding(Queue vacancyAiQueue, TopicExchange vacancyExchange) {
        return BindingBuilder.bind(vacancyAiQueue).to(vacancyExchange).with(VACANCY_FLYER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
