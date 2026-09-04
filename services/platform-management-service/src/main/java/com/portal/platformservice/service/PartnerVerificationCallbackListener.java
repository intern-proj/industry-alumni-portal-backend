package com.portal.platformservice.service;

import com.portal.platformservice.config.RabbitMQConfig;
import com.portal.platformservice.dto.message.UpdateEmailDTO;
import com.portal.platformservice.entity.PartnerVerification;
import com.portal.platformservice.event.PartnerVerificationDecidedEvent;
import com.portal.platformservice.repository.PartnerVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerVerificationCallbackListener {

    private final PartnerVerificationRepository partnerVerificationRepository;
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecided(PartnerVerificationDecidedEvent event) {
        log.warn("Partner verification {} decided as {}, but UserServiceClient is not yet implemented; "
                        + "leaving syncStatus=PENDING_CALLBACK for CallbackRetryScheduler to retry",
                event.verificationId(), event.outcome());

        try {
            partnerVerificationRepository.findById(event.verificationId()).ifPresent(verification -> {
                UpdateEmailDTO emailDTO = new UpdateEmailDTO();
                emailDTO.setToEmail(verification.getContactEmailSnapshot());
                emailDTO.setRecipientName(verification.getOrganizationNameSnapshot());

                if (event.outcome().name().equals("APPROVED")) {
                    emailDTO.setUpdateType(UpdateEmailDTO.UpdateType.PROFILE_APPROVED);
                    emailDTO.setUpdateBody("Congratulations! Your industry partner verification has been approved. You can now access all portal features.");
                } else {
                    emailDTO.setUpdateType(UpdateEmailDTO.UpdateType.GENERAL_UPDATE);
                    emailDTO.setUpdateBody("Your industry partner verification was declined or requires changes. Please log in to the portal for more details.");
                }

                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "update.queue", emailDTO);
                log.info("Sent email notification for verification decision to {}", emailDTO.getToEmail());
            });
        } catch (Exception ex) {
            log.error("Failed to send email notification for verification decision: {}", ex.getMessage());
        }
    }
}
