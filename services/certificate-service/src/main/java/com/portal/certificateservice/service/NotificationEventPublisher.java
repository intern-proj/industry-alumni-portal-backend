package com.portal.certificateservice.service;

import com.portal.certificateservice.config.RabbitMQConfig;
import com.portal.certificateservice.dto.CertificateNotificationEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCertificateNotification(CertificateNotificationEventDto event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.CERTIFICATE_ROUTING_KEY,
                    event
            );
            log.info("Published notification.certificate event for certificate ID: {}", event.getCertificateId());
        } catch (Exception e) {
            log.warn("Could not publish RabbitMQ notification event: {}", e.getMessage());
        }
    }
}
