package com.nsbm.notification_service;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.listener.OtpListener;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import com.nsbm.notification_service.service.handlers.OtpSendingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceApplicationTest {

    @Mock
    private EmailDeliveryService emailDeliveryService;

    @Mock
    private OtpSendingService otpSendingService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OtpListener otpListener;

    @Test
    void test_SendOtp_ListenerProcessing() {
        OtpEmailDTO otp = new OtpEmailDTO(
                "prasadkvithana@gmail.com",
                "123456"
        );

        when(otpSendingService.OtpProcessing(otp)).thenReturn(true);

        otpListener.handleOTP(otp);

        verify(otpSendingService).OtpProcessing(otp);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.otp"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertEquals("prasadkvithana@gmail.com", statusDTO.getToEmail());
        assertTrue(statusDTO.getStatus());
    }

    @Test
    void test_OtpSendingService_Processing() {
        OtpSendingService sendingService = new OtpSendingService(emailDeliveryService);

        OtpEmailDTO otp = new OtpEmailDTO("prasadkvithana@gmail.com", "123456");
        doNothing().when(emailDeliveryService).sendHtmlEmail(anyString(), anyString(), anyString());

        boolean result = sendingService.OtpProcessing(otp);

        assertTrue(result);
        verify(emailDeliveryService).sendHtmlEmail(eq("prasadkvithana@gmail.com"), anyString(), anyString());
    }
}