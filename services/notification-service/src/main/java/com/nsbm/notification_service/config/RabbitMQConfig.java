package com.nsbm.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "notification.exchange";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue otpQueue() {
        return new Queue("otp.queue", true);
    }

    @Bean
    public Queue remindersQueue() {
        return new Queue("reminders.queue", true);
    }

    @Bean
    public Queue invitationQueue() {
        return new Queue("invitation.queue", true);
    }

    @Bean
    public Queue announcementQueue() {
        return new Queue("announcement.queue", true);
    }

    @Bean
    public Queue updateQueue() {
        return new Queue("update.queue", true);
    }


    @Bean
    public Queue otpStatusQueue() {
        return new Queue("otp.status.queue", true);
    }

    @Bean
    public Queue announcementStatusQueue() {
        return new Queue("announcement.status.queue", true);
    }

    @Bean
    public Queue invitationStatusQueue() {
        return new Queue("invitation.status.queue", true);
    }

    @Bean
    public Queue reminderStatusQueue() {
        return new Queue("reminder.status.queue", true);
    }

    @Bean
    public Queue updateStatusQueue() {
        return new Queue("update.status.queue", true);
    }


    @Bean
    public Binding otpBinding(Queue otpQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(otpQueue).to(notificationExchange).with("notification.otp");
    }

    @Bean
    public Binding remindersBinding(Queue remindersQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(remindersQueue).to(notificationExchange).with("notification.reminder");
    }

    @Bean
    public Binding invitationBinding(Queue invitationQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(invitationQueue).to(notificationExchange).with("notification.invitation");
    }

    @Bean
    public Binding announcementBinding(Queue announcementQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(announcementQueue).to(notificationExchange).with("notification.announcement");
    }

    @Bean
    public Binding updateBinding(Queue updateQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(updateQueue).to(notificationExchange).with("notification.update");
    }


    @Bean
    public Binding otpStatusBinding(Queue otpStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(otpStatusQueue).to(notificationExchange).with("notification.status.otp");
    }

    @Bean
    public Binding announcementStatusBinding(Queue announcementStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(announcementStatusQueue).to(notificationExchange).with("notification.status.announcement");
    }

    @Bean
    public Binding invitationStatusBinding(Queue invitationStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(invitationStatusQueue).to(notificationExchange).with("notification.status.invitation");
    }

    @Bean
    public Binding reminderStatusBinding(Queue reminderStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(reminderStatusQueue).to(notificationExchange).with("notification.status.reminder");
    }

    @Bean
    public Binding updateStatusBinding(Queue updateStatusQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(updateStatusQueue).to(notificationExchange).with("notification.status.update");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter("com.nsbm.notification_service.dto");
    }
}
