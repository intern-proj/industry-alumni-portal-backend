package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.InvitationEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.service.handlers.InvitationSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationListener {

    private final InvitationSendingService invitationSendingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "invitation.queue")
    public void handleInvitation(InvitationEmailDTO invitation) {
        OtpEmailStatusDTO statusDTO = new OtpEmailStatusDTO();
        String recipient = (invitation != null && invitation.getToEmail() != null) ? invitation.getToEmail() : "unknown";
        statusDTO.setToEmail(recipient);

        try {
            if (invitation != null) {
                log.info("Received invitation for event '{}' to {}", invitation.getEventName(), recipient);
            }
            statusDTO.setStatus(invitationSendingService.invitationProcessing(invitation));
            log.info("Successfully processed invitation notification for {}", recipient);
        } catch (Exception ex) {
            log.error("Invitation processing failed for {}: {}", recipient, ex.getMessage(), ex);
            statusDTO.setStatus(false);
            statusDTO.setError(ex.getMessage() != null ? ex.getMessage() : "Unknown error processing invitation");
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    "notification.status.invitation",
                    statusDTO
            );
        } catch (Exception ex) {
            log.error("Failed to publish status back to RabbitMQ for invitation to {}: {}", recipient, ex.getMessage());
        }
    }

}
