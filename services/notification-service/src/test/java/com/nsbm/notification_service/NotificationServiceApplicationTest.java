package com.nsbm.notification_service;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
public class NotificationServiceApplicationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void test_SendOtp() {
        OtpEmailDTO otp = new OtpEmailDTO(
                "prasadkvithana@gmail.com",
                "123456"
        );

        // 1. Asynchronously publish OTP request event (Fire and Forget)
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                "notification.otp",
                otp
        );

        log.info("OTP request published asynchronously. Waiting for status response...");

        // 2. Poll the status queue for up to 15 seconds to receive the processed status event
        OtpEmailStatusDTO statusDTO = (OtpEmailStatusDTO) rabbitTemplate.receiveAndConvert(
                "otp.status.queue",
                15000 // Timeout in milliseconds
        );

        // 3. Assertions
        assertNotNull(statusDTO, "Timed out waiting for delivery status from notification.status.otp!");

        if (Boolean.TRUE.equals(statusDTO.getStatus())) {
            log.info("OTP Test Executed Successfully for recipient: {}", statusDTO.getToEmail());
        } else {
            log.info("Something went wrong when sending the OTP code to {}", statusDTO.getToEmail());
            log.error("Error details: {}", statusDTO.getError());
        }

        assertTrue(statusDTO.getStatus(), "Email delivery failed: " + statusDTO.getError());
    }
}