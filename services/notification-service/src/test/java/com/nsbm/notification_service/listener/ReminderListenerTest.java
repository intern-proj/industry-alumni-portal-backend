package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.dto.ReminderEmailDTO;
import com.nsbm.notification_service.service.handlers.ReminderSendingService;
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
class ReminderListenerTest {

    @Mock
    private ReminderSendingService reminderSendingService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ReminderListener reminderListener;

    @Test
    void test_handleReminder_EventType_SuccessPublishesStatusTrue() {
        ReminderEmailDTO dto = new ReminderEmailDTO(
                "student@example.com",
                "Alice",
                ReminderEmailDTO.ReminderType.EVENT,
                "Career Fair 2026",
                "Don't miss the upcoming Career Fair at NSBM.",
                "12th August 2026",
                "https://portal.example.com/events/1"
        );

        when(reminderSendingService.reminderProcessing(dto)).thenReturn(true);

        reminderListener.handleReminder(dto);

        verify(reminderSendingService).reminderProcessing(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.reminder"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertEquals("student@example.com", statusDTO.getToEmail());
        assertTrue(statusDTO.getStatus());
    }

    @Test
    void test_handleReminder_DeadlineType_ExceptionPublishesStatusFalse() {
        ReminderEmailDTO dto = new ReminderEmailDTO(
                "student@example.com",
                "Bob",
                ReminderEmailDTO.ReminderType.DEADLINE,
                "Assignment Submission",
                "Your assignment is due soon.",
                "10th Aug 2026",
                null
        );

        when(reminderSendingService.reminderProcessing(dto))
                .thenThrow(new RuntimeException("Mail host unreachable"));

        reminderListener.handleReminder(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.reminder"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertFalse(statusDTO.getStatus());
        assertNotNull(statusDTO.getError());
    }
}
