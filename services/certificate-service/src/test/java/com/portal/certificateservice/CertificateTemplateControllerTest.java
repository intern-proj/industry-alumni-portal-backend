package com.portal.certificateservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.certificateservice.controller.CertificateTemplateController;
import com.portal.certificateservice.dto.CreateTemplateRequestDto;
import com.portal.certificateservice.dto.TemplateResponseDto;
import com.portal.certificateservice.dto.UpdateTemplateRequestDto;
import com.portal.certificateservice.exception.GlobalExceptionHandler;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.service.CertificateTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CertificateTemplateController.class)
@Import(GlobalExceptionHandler.class)
class CertificateTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CertificateTemplateService templateService;

    private UUID templateId;
    private TemplateResponseDto templateResponse;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        templateResponse = new TemplateResponseDto(
                templateId, "Workshop Template", "/templates/bg.png", "{}", true, LocalDateTime.now()
        );
    }

    @Test
    void testCreateTemplate_Success() throws Exception {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto(
                "Workshop Template", "/templates/bg.png", "{}", true
        );

        when(templateService.createTemplate(any())).thenReturn(templateResponse);

        mockMvc.perform(post("/api/v1/certificate-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(templateId.toString()))
                .andExpect(jsonPath("$.templateName").value("Workshop Template"));
    }

    @Test
    void testGetAllTemplates_Success() throws Exception {
        when(templateService.getAllTemplates()).thenReturn(List.of(templateResponse));

        mockMvc.perform(get("/api/v1/certificate-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateName").value("Workshop Template"));
    }

    @Test
    void testGetActiveTemplates_Success() throws Exception {
        when(templateService.getActiveTemplates()).thenReturn(List.of(templateResponse));

        mockMvc.perform(get("/api/v1/certificate-templates?activeOnly=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void testGetTemplateById_Success() throws Exception {
        when(templateService.getTemplateById(templateId)).thenReturn(templateResponse);

        mockMvc.perform(get("/api/v1/certificate-templates/{id}", templateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(templateId.toString()));
    }

    @Test
    void testGetTemplateById_NotFound() throws Exception {
        when(templateService.getTemplateById(templateId)).thenThrow(new ResourceNotFoundException("Template not found"));

        mockMvc.perform(get("/api/v1/certificate-templates/{id}", templateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void testUpdateTemplate_Success() throws Exception {
        UpdateTemplateRequestDto request = new UpdateTemplateRequestDto();
        request.setTemplateName("Updated Template");

        when(templateService.updateTemplate(eq(templateId), any())).thenReturn(templateResponse);

        mockMvc.perform(put("/api/v1/certificate-templates/{id}", templateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteTemplate_Success() throws Exception {
        doNothing().when(templateService).deleteTemplate(templateId);

        mockMvc.perform(delete("/api/v1/certificate-templates/{id}", templateId))
                .andExpect(status().isNoContent());
    }
}
