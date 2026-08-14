package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.service.handlers.OtpSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpListener {

    private final OtpSendingService otpSendingService;
    private final RabbitTemplate rabbitTemplate;


    @RabbitListener(queues = "otp.queue")
    public void handleOTP(OtpEmailDTO otp) {
        OtpEmailStatusDTO statusDTO = new OtpEmailStatusDTO();
        String recipient = (otp != null && otp.getToEmail() != null) ? otp.getToEmail() : "unknown";
        statusDTO.setToEmail(recipient);

        try {
            statusDTO.setStatus(otpSendingService.OtpProcessing(otp));
            log.info("Successfully processed OTP notification for {}", recipient);
        } catch (Exception ex) {
            log.error("Failed to process OTP notification for {}: {}", recipient, ex.getMessage(), ex);
            statusDTO.setStatus(false);
            statusDTO.setError(ex.getMessage() != null ? ex.getMessage() : "Unknown error processing OTP");
        }

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "notification.status.otp", statusDTO);
        } catch (Exception ex) {
            log.error("Failed to publish status back to RabbitMQ for OTP to {}: {}", recipient, ex.getMessage());
        }
    }



}
