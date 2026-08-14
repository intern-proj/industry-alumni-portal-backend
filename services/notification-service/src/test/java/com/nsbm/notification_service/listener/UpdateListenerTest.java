package com.nsbm.notification_service.listener;

import com.nsbm.notification_service.config.RabbitMQConfig;
import com.nsbm.notification_service.dto.OtpEmailStatusDTO;
import com.nsbm.notification_service.dto.UpdateEmailDTO;
import com.nsbm.notification_service.service.handlers.UpdateSendingService;
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
class UpdateListenerTest {

    @Mock
    private UpdateSendingService updateSendingService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UpdateListener updateListener;

    @Test
    void test_handleUpdate_ProfileApproved_SuccessPublishesStatusTrue() {
        UpdateEmailDTO dto = new UpdateEmailDTO(
                "user@example.com",
                "Charlie",
                UpdateEmailDTO.UpdateType.PROFILE_APPROVED,
                "Congratulations! Your profile has been approved and is now visible to industry partners.",
                "https://portal.example.com/profile"
        );

        when(updateSendingService.updateProcessing(dto)).thenReturn(true);

        updateListener.handleUpdate(dto);

        verify(updateSendingService).updateProcessing(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.update"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertEquals("user@example.com", statusDTO.getToEmail());
        assertTrue(statusDTO.getStatus());
        assertNull(statusDTO.getError());
    }

    @Test
    void test_handleUpdate_JobPosted_ExceptionPublishesStatusFalse() {
        UpdateEmailDTO dto = new UpdateEmailDTO(
                "user@example.com",
                "Diana",
                UpdateEmailDTO.UpdateType.JOB_POSTED,
                "A new Software Engineer role has been posted.",
                "https://portal.example.com/jobs/42"
        );

        when(updateSendingService.updateProcessing(dto))
                .thenThrow(new RuntimeException("Connection timeout"));

        updateListener.handleUpdate(dto);

        ArgumentCaptor<OtpEmailStatusDTO> captor = ArgumentCaptor.forClass(OtpEmailStatusDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("notification.status.update"),
                captor.capture()
        );

        OtpEmailStatusDTO statusDTO = captor.getValue();
        assertFalse(statusDTO.getStatus());
        assertNotNull(statusDTO.getError());
    }
}
