package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.dto.ReminderEmailDTO;
import com.nsbm.notification_service.service.handlers.ReminderSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderListener {

    private final ReminderSendingService reminderSendingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "reminders.queue")
    public void handleReminder(ReminderEmailDTO reminder) {
        OtpEmailStatusDTO statusDTO = new OtpEmailStatusDTO();
        String recipient = (reminder != null && reminder.getToEmail() != null) ? reminder.getToEmail() : "unknown";
        statusDTO.setToEmail(recipient);

        try {
            if (reminder != null) {
                log.info("Received {} reminder for {}", reminder.getReminderType(), recipient);
            }
            statusDTO.setStatus(reminderSendingService.reminderProcessing(reminder));
            log.info("Successfully processed reminder notification for {}", recipient);
        } catch (Exception ex) {
            log.error("Reminder processing failed for {}: {}", recipient, ex.getMessage(), ex);
            statusDTO.setStatus(false);
            statusDTO.setError(ex.getMessage() != null ? ex.getMessage() : "Unknown error processing reminder");
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "notification.status.reminder",
                    statusDTO
            );
        } catch (Exception ex) {
            log.error("Failed to publish status back to RabbitMQ for reminder to {}: {}", recipient, ex.getMessage());
        }
    }

}
