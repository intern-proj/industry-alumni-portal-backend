package com.portal.certificateservice;

import com.portal.certificateservice.dto.CreateTemplateRequestDto;
import com.portal.certificateservice.dto.TemplateResponseDto;
import com.portal.certificateservice.dto.UpdateTemplateRequestDto;
import com.portal.certificateservice.entity.CertificateTemplate;
import com.portal.certificateservice.exception.CertificateException;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.repository.CertificateTemplateRepository;
import com.portal.certificateservice.service.CertificateTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateTemplateServiceTest {

    @Mock
    private CertificateTemplateRepository templateRepository;

    @InjectMocks
    private CertificateTemplateService templateService;

    private UUID templateId;
    private CertificateTemplate template;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        template = new CertificateTemplate("Default Template", "/tpl.pdf", "{}", true);
        template.setId(templateId);
    }

    @Test
    void testCreateTemplate_Success() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto("Default Template", "/tpl.pdf", "{}", true);

        when(templateRepository.existsByTemplateName("Default Template")).thenReturn(false);
        when(templateRepository.save(any())).thenReturn(template);

        TemplateResponseDto response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals("Default Template", response.getTemplateName());
        assertTrue(response.getIsActive());
    }

    @Test
    void testCreateTemplate_Duplicate_ThrowsException() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto("Default Template", "/tpl.pdf", "{}", true);

        when(templateRepository.existsByTemplateName("Default Template")).thenReturn(true);

        assertThrows(CertificateException.class, () -> templateService.createTemplate(request));
    }

    @Test
    void testGetTemplateById_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        TemplateResponseDto response = templateService.getTemplateById(templateId);

        assertEquals(templateId, response.getId());
        assertEquals("Default Template", response.getTemplateName());
    }

    @Test
    void testGetTemplateById_NotFound_ThrowsException() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getTemplateById(templateId));
    }

    @Test
    void testUpdateTemplate_Success() {
        UpdateTemplateRequestDto updateRequest = new UpdateTemplateRequestDto();
        updateRequest.setTemplateName("Renamed Template");

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any())).thenReturn(template);

        TemplateResponseDto response = templateService.updateTemplate(templateId, updateRequest);

        assertEquals("Renamed Template", response.getTemplateName());
    }

    @Test
    void testDeleteTemplate_Success() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        doNothing().when(templateRepository).deleteById(templateId);

        assertDoesNotThrow(() -> templateService.deleteTemplate(templateId));
        verify(templateRepository, times(1)).deleteById(templateId);
    }
}
