package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.AnnouncementEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.service.handlers.AnnouncementSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnouncementListener {

    private final AnnouncementSendingService announcementSendingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "announcement.queue")
    public void handleAnnouncement(AnnouncementEmailDTO announcement) {
        OtpEmailStatusDTO statusDTO = new OtpEmailStatusDTO();
        String sender = (announcement != null && announcement.getSenderName() != null) ? announcement.getSenderName() : "unknown";
        statusDTO.setToEmail(sender);

        try {
            if (announcement != null) {
                log.info("Received announcement '{}' for {} recipients",
                        announcement.getAnnouncementTitle(),
                        announcement.getToEmails() != null ? announcement.getToEmails().size() : 0);
            }
            boolean result = announcementSendingService.announcementProcessing(announcement);
            statusDTO.setStatus(result);
            log.info("Finished processing announcement for sender {}", sender);
        } catch (Exception ex) {
            log.error("Announcement processing failed for sender {}: {}", sender, ex.getMessage(), ex);
            statusDTO.setStatus(false);
            statusDTO.setError(ex.getMessage() != null ? ex.getMessage() : "Unknown error processing announcement");
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "notification.status.announcement",
                    statusDTO
            );
        } catch (Exception ex) {
            log.error("Failed to publish status back to RabbitMQ for announcement sender {}: {}", sender, ex.getMessage());
        }
    }

}
