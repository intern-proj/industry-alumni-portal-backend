package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.AnnouncementEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.service.handlers.AnnouncementSendingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementListenerTest {

    @Mock
    private AnnouncementSendingService announcementSendingService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AnnouncementListener announcementListener;

    @Test
    void test_handleAnnouncement_SuccessPublishesStatusTrue() {
        AnnouncementEmailDTO dto = new AnnouncementEmailDTO(
                List.of("user1@example.com", "user2@example.com"),
                "Annual Alumni Meet",
                "Join us for the annual alumni gathering!",
                "Admin",
                "https://portal.example.com"
        );

        when(announcementSendingService.announcementProcessing(dto)).thenReturn(true);

        announcementListener.handleAnnouncement(dto);

        verify(announcementSendingService).announcementProcessing(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.announcement"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertEquals("Admin", statusDTO.getToEmail());
        assertTrue(statusDTO.getStatus());
        assertNull(statusDTO.getError());
    }

    @Test
    void test_handleAnnouncement_ExceptionPublishesStatusFalse() {
        AnnouncementEmailDTO dto = new AnnouncementEmailDTO(
                List.of("user@example.com"),
                "Test Announcement",
                "Body text",
                "Admin",
                null
        );

        when(announcementSendingService.announcementProcessing(dto))
                .thenThrow(new RuntimeException("SMTP connection refused"));

        announcementListener.handleAnnouncement(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.announcement"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertFalse(statusDTO.getStatus());
        assertNotNull(statusDTO.getError());
    }
}
