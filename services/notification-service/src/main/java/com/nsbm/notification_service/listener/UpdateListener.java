package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.dto.UpdateEmailDTO;
import com.nsbm.notification_service.service.handlers.UpdateSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateListener {

    private final UpdateSendingService updateSendingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "update.queue")
    public void handleUpdate(UpdateEmailDTO update) {
        OtpEmailStatusDTO statusDTO = new OtpEmailStatusDTO();
        String recipient = (update != null && update.getToEmail() != null) ? update.getToEmail() : "unknown";
        statusDTO.setToEmail(recipient);

        try {
            if (update != null) {
                log.info("Received {} update notification for {}", update.getUpdateType(), recipient);
            }
            statusDTO.setStatus(updateSendingService.updateProcessing(update));
            log.info("Successfully processed update notification for {}", recipient);
        } catch (Exception ex) {
            log.error("Update notification processing failed for {}: {}", recipient, ex.getMessage(), ex);
            statusDTO.setStatus(false);
            statusDTO.setError(ex.getMessage() != null ? ex.getMessage() : "Unknown error processing update notification");
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "notification.status.update",
                    statusDTO
            );
        } catch (Exception ex) {
            log.error("Failed to publish status back to RabbitMQ for update to {}: {}", recipient, ex.getMessage());
        }
    }

}
