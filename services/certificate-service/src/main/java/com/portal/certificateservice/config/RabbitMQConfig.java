package com.portal.certificateservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_DLX = "notification.dlx";

    public static final String CERTIFICATE_QUEUE = "certificate.queue";
    public static final String CERTIFICATE_ROUTING_KEY = "notification.certificate";
    public static final String CERTIFICATE_DLQ = "certificate.dlq";

    public static final String EVENT_COMPLETED_QUEUE = "event.completed.queue";
    public static final String EVENT_COMPLETED_ROUTING_KEY = "event.completed";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue certificateQueue() {
        return QueueBuilder.durable(CERTIFICATE_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", CERTIFICATE_DLQ)
                .build();
    }

    @Bean
    public Queue certificateDlq() {
        return QueueBuilder.durable(CERTIFICATE_DLQ).build();
    }

    @Bean
    public Binding certificateBinding(Queue certificateQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(certificateQueue).to(notificationExchange).with(CERTIFICATE_ROUTING_KEY);
    }

    @Bean
    public Queue eventCompletedQueue() {
        return QueueBuilder.durable(EVENT_COMPLETED_QUEUE).build();
    }

    @Bean
    public Binding eventCompletedBinding(Queue eventCompletedQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(eventCompletedQueue).to(notificationExchange).with(EVENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
