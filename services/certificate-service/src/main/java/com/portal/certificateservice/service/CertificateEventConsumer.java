package com.portal.certificateservice.service;

import com.portal.certificateservice.config.RabbitMQConfig;
import com.portal.certificateservice.dto.BulkGenerateCertificateRequestDto;
import com.portal.certificateservice.dto.EventCompletedEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class CertificateEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CertificateEventConsumer.class);
    private final CertificateService certificateService;

    public CertificateEventConsumer(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @RabbitListener(queues = RabbitMQConfig.EVENT_COMPLETED_QUEUE)
    public void consumeEventCompleted(EventCompletedEventDto event) {
        log.info("Received event.completed for event ID: {}", event.getEventId());
        if (event.getEligibleStudentIds() == null || event.getEligibleStudentIds().isEmpty()) {
            log.info("No eligible students to generate certificates for event ID: {}", event.getEventId());
            return;
        }

        BulkGenerateCertificateRequestDto request = new BulkGenerateCertificateRequestDto(
                event.getEventId(),
                event.getTemplateId(),
                event.getEligibleStudentIds(),
                event.getEventTitle()
        );

        certificateService.bulkGenerateCertificates(request);
        log.info("Successfully triggered batch certificate generation for event ID: {}", event.getEventId());
    }
}
