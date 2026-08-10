package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.service.handlers.OtpSendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpListener {

    private final OtpSendingService otpSendingService;
    private final RabbitTemplate rabbitTemplate;


    @RabbitListener(queues = "otp.queue")
    public void handleOTP(OtpEmailDTO otp){

        OtpEmailStatusDTO statusDTO = new OtpEmailStatusDTO();
        statusDTO.setToEmail(otp.getToEmail());

        try {
             statusDTO.setStatus(otpSendingService.OtpProcessing(otp));
        }catch (Exception ex){

            statusDTO.setStatus(false);
            statusDTO.setError(ex.getLocalizedMessage());

        }

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,"notification.status.otp",statusDTO);

    }


}
