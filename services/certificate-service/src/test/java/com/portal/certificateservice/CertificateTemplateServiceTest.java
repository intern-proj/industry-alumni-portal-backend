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

import java.time.LocalDateTime;
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
        template = new CertificateTemplate("Standard Template", "/templates/bg.png", "{}", true);
        template.setId(templateId);
    }

    @Test
    void testCreateTemplate_Success() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto("Standard Template", "/templates/bg.png", "{}", true);

        when(templateRepository.existsByTemplateName("Standard Template")).thenReturn(false);
        when(templateRepository.save(any())).thenReturn(template);

        TemplateResponseDto response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals("Standard Template", response.getTemplateName());
    }

    @Test
    void testCreateTemplate_DuplicateName_ThrowsException() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto("Standard Template", "/templates/bg.png", "{}", true);

        when(templateRepository.existsByTemplateName("Standard Template")).thenReturn(true);

        assertThrows(CertificateException.class, () -> templateService.createTemplate(request));
    }

    @Test
    void testGetAllTemplates_Success() {
        when(templateRepository.findAll()).thenReturn(List.of(template));

        List<TemplateResponseDto> list = templateService.getAllTemplates();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
    }

    @Test
    void testGetActiveTemplates_Success() {
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(template));

        List<TemplateResponseDto> list = templateService.getActiveTemplates();

        assertFalse(list.isEmpty());
        assertTrue(list.get(0).getIsActive());
    }

    @Test
    void testGetTemplateById_Success() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        TemplateResponseDto dto = templateService.getTemplateById(templateId);

        assertNotNull(dto);
        assertEquals(templateId, dto.getId());
    }

    @Test
    void testDeleteTemplate_Success() {
        when(templateRepository.existsById(templateId)).thenReturn(true);
        doNothing().when(templateRepository).deleteById(templateId);

        assertDoesNotThrow(() -> templateService.deleteTemplate(templateId));
        verify(templateRepository, times(1)).deleteById(templateId);
    }
}
