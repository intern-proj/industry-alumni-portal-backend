package com.portal.certificateservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.certificateservice.controller.CertificateController;
import com.portal.certificateservice.dto.*;
import com.portal.certificateservice.exception.GlobalExceptionHandler;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.service.CertificateService;
import com.portal.certificateservice.service.CertificateTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CertificateControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CertificateService certificateService;

    @Mock
    private CertificateTemplateService templateService;

    @InjectMocks
    private CertificateController certificateController;

    private UUID studentId;
    private UUID eventId;
    private UUID templateId;
    private UUID certId;
    private CertificateResponseDto certResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(certificateController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        studentId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        certId = UUID.randomUUID();

        certResponse = new CertificateResponseDto(
                certId, studentId, eventId, templateId,
                "CERT-12345678", "/storage/cert.pdf", "ISSUED", LocalDateTime.now()
        );
    }

    @Test
    void testUploadTemplate_Success() throws Exception {
        CreateTemplateRequestDto request = new CreateTemplateRequestDto("Default Template", "/templates/default.png", "{}", true);
        TemplateResponseDto templateResponse = new TemplateResponseDto(templateId, "Default Template", "/templates/default.png", "{}", true, LocalDateTime.now());

        when(templateService.createTemplate(any())).thenReturn(templateResponse);

        mockMvc.perform(post("/api/v1/certificates/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(templateId.toString()))
                .andExpect(jsonPath("$.templateName").value("Default Template"));
    }

    @Test
    void testGetAllTemplates_Success() throws Exception {
        TemplateResponseDto templateResponse = new TemplateResponseDto(templateId, "Default Template", "/templates/default.png", "{}", true, LocalDateTime.now());
        when(templateService.getAllTemplates()).thenReturn(List.of(templateResponse));

        mockMvc.perform(get("/api/v1/certificates/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateName").value("Default Template"));
    }

    @Test
    void testGenerateCertificate_Success() throws Exception {
        GenerateCertificateRequestDto request = new GenerateCertificateRequestDto(
                studentId, eventId, templateId, "John Doe", "Workshop"
        );

        when(certificateService.generateCertificate(any())).thenReturn(certResponse);

        mockMvc.perform(post("/api/v1/certificates/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(certId.toString()))
                .andExpect(jsonPath("$.verificationCode").value("CERT-12345678"));
    }

    @Test
    void testBulkGenerateCertificates_Success() throws Exception {
        BulkGenerateCertificateRequestDto request = new BulkGenerateCertificateRequestDto(
                eventId, templateId, List.of(studentId), "Workshop"
        );

        when(certificateService.bulkGenerateCertificates(any())).thenReturn(List.of(certResponse));

        mockMvc.perform(post("/api/v1/certificates/batch-generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(certId.toString()));
    }

    @Test
    void testGetStudentCertificates_Success() throws Exception {
        when(certificateService.getCertificatesByStudent(studentId)).thenReturn(List.of(certResponse));

        mockMvc.perform(get("/api/v1/certificates/student/{studentId}", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(studentId.toString()));
    }

    @Test
    void testGetCertificateById_Success() throws Exception {
        when(certificateService.getCertificateById(certId)).thenReturn(certResponse);

        mockMvc.perform(get("/api/v1/certificates/{id}", certId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(certId.toString()));
    }

    @Test
    void testGetCertificateById_NotFound() throws Exception {
        when(certificateService.getCertificateById(certId)).thenThrow(new ResourceNotFoundException("Certificate not found"));

        mockMvc.perform(get("/api/v1/certificates/{id}", certId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void testDownloadCertificatePdf_Success() throws Exception {
        File tempPdf = File.createTempFile("test_cert", ".pdf");
        Files.write(tempPdf.toPath(), "Dummy PDF content".getBytes());
        tempPdf.deleteOnExit();

        when(certificateService.getCertificatePdfFile(certId)).thenReturn(tempPdf);

        mockMvc.perform(get("/api/v1/certificates/{id}/download", certId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void testUpdateStatus_Success() throws Exception {
        CertificateResponseDto revokedCert = new CertificateResponseDto(
                certId, studentId, eventId, templateId,
                "CERT-12345678", "/storage/cert.pdf", "REVOKED", LocalDateTime.now()
        );

        when(certificateService.updateCertificateStatus(eq(certId), eq("REVOKED"))).thenReturn(revokedCert);

        mockMvc.perform(patch("/api/v1/certificates/{id}/status", certId)
                .param("status", "REVOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    void testVerifyCertificate_Success() throws Exception {
        CertificateVerificationResponseDto verificationResponse = new CertificateVerificationResponseDto(
                true, certId, "CERT-12345678", studentId, eventId, templateId, "ISSUED", LocalDateTime.now(), "Certificate is authentic"
        );

        when(certificateService.verifyCertificate(eq("CERT-12345678"), any())).thenReturn(verificationResponse);

        mockMvc.perform(get("/api/v1/certificates/verify/{qrHash}", "CERT-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.verificationCode").value("CERT-12345678"));
    }

    @Test
    void testGetStats_Success() throws Exception {
        CertificateStatsDto statsDto = new CertificateStatsDto(10, 2, 25);

        when(certificateService.getStats()).thenReturn(statsDto);

        mockMvc.perform(get("/api/v1/certificates/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCertificatesIssued").value(10))
                .andExpect(jsonPath("$.totalActiveTemplates").value(2))
                .andExpect(jsonPath("$.totalVerificationsCount").value(25));
    }
}
