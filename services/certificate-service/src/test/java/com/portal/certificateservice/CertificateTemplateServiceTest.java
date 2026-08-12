package com.portal.certificateservice;

import com.portal.certificateservice.dto.CreateTemplateRequestDto;
import com.portal.certificateservice.dto.TemplateResponseDto;
import com.portal.certificateservice.entity.CertificateTemplate;
import com.portal.certificateservice.repository.CertificateTemplateRepository;
import com.portal.certificateservice.service.CertificateTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void testCreateTemplate_Success() {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto("Default Template", "/tpl.pdf", "{}", true);

        when(templateRepository.existsByTemplateName("Default Template")).thenReturn(false);
        
        CertificateTemplate saved = new CertificateTemplate("Default Template", "/tpl.pdf", "{}", true);
        saved.setId(UUID.randomUUID());
        when(templateRepository.save(any())).thenReturn(saved);

        TemplateResponseDto response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals("Default Template", response.getTemplateName());
        assertTrue(response.getIsActive());
    }
}
