package com.nsbm.notification_service.service;

import com.nsbm.notification_service.dto.NotificationTemplateDTO;
import com.nsbm.notification_service.exception.DuplicateTemplateException;
import com.nsbm.notification_service.exception.TemplateNotFoundException;
import com.nsbm.notification_service.model.NotificationTemplate;
import com.nsbm.notification_service.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private NotificationTemplateService templateService;

    @Test
    void test_getAllTemplates() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(1L)
                .templateCode("OTP_EMAIL")
                .name("OTP Template")
                .subject("Your OTP")
                .body("<p>Code: {{code}}</p>")
                .createdAt(LocalDateTime.now())
                .build();

        when(templateRepository.findAll()).thenReturn(List.of(template));

        List<NotificationTemplateDTO> result = templateService.getAllTemplates();

        assertEquals(1, result.size());
        assertEquals("OTP_EMAIL", result.get(0).getTemplateCode());
    }

    @Test
    void test_getTemplateById_Success() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(1L)
                .templateCode("OTP_EMAIL")
                .name("OTP Template")
                .subject("Your OTP")
                .body("<p>Code: {{code}}</p>")
                .build();

        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        NotificationTemplateDTO dto = templateService.getTemplateById(1L);

        assertNotNull(dto);
        assertEquals("OTP_EMAIL", dto.getTemplateCode());
    }

    @Test
    void test_getTemplateById_NotFoundThrowsException() {
        when(templateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TemplateNotFoundException.class, () -> templateService.getTemplateById(99L));
    }

    @Test
    void test_createTemplate_Success() {
        NotificationTemplateDTO inputDto = NotificationTemplateDTO.builder()
                .templateCode("NEW_TEMPLATE")
                .name("New Template")
                .subject("Subject")
                .body("<p>Body</p>")
                .build();

        NotificationTemplate savedEntity = NotificationTemplate.builder()
                .id(10L)
                .templateCode("NEW_TEMPLATE")
                .name("New Template")
                .subject("Subject")
                .body("<p>Body</p>")
                .createdAt(LocalDateTime.now())
                .build();

        when(templateRepository.existsByTemplateCode("NEW_TEMPLATE")).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(savedEntity);

        NotificationTemplateDTO result = templateService.createTemplate(inputDto);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("NEW_TEMPLATE", result.getTemplateCode());
    }

    @Test
    void test_createTemplate_DuplicateCodeThrowsException() {
        NotificationTemplateDTO inputDto = NotificationTemplateDTO.builder()
                .templateCode("EXISTING_CODE")
                .build();

        when(templateRepository.existsByTemplateCode("EXISTING_CODE")).thenReturn(true);

        assertThrows(DuplicateTemplateException.class, () -> templateService.createTemplate(inputDto));
    }

    @Test
    void test_deleteTemplate_Success() {
        when(templateRepository.existsById(1L)).thenReturn(true);
        doNothing().when(templateRepository).deleteById(1L);

        assertDoesNotThrow(() -> templateService.deleteTemplate(1L));

        verify(templateRepository).deleteById(1L);
    }
}
