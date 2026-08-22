package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.InvitationEmailDTO;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.service.handlers.InvitationSendingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationListenerTest {

    @Mock
    private InvitationSendingService invitationSendingService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InvitationListener invitationListener;

    @Test
    void test_handleInvitation_SuccessPublishesStatusTrue() {
        InvitationEmailDTO dto = new InvitationEmailDTO(
                "attendee@example.com",
                "John Doe",
                "Alumni Tech Fest 2026",
                "15th September 2026, 10:00 AM",
                "NSBM Green University",
                "Annual technology showcase for alumni.",
                "https://portal.example.com/rsvp/123",
                "Event Team"
        );

        when(invitationSendingService.invitationProcessing(dto)).thenReturn(true);

        invitationListener.handleInvitation(dto);

        verify(invitationSendingService).invitationProcessing(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.invitation"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertEquals("attendee@example.com", statusDTO.getToEmail());
        assertTrue(statusDTO.getStatus());
        assertNull(statusDTO.getError());
    }

    @Test
    void test_handleInvitation_ExceptionPublishesStatusFalse() {
        InvitationEmailDTO dto = new InvitationEmailDTO(
                "attendee@example.com",
                "Jane Doe",
                "Tech Summit",
                "20th Oct 2026",
                null,
                null,
                null,
                null
        );

        when(invitationSendingService.invitationProcessing(dto))
                .thenThrow(new RuntimeException("Email server unavailable"));

        invitationListener.handleInvitation(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.invitation"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertFalse(statusDTO.getStatus());
        assertNotNull(statusDTO.getError());
    }
}
