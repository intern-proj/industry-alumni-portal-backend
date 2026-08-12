package com.portal.certificateservice;

import com.portal.certificateservice.dto.CertificateResponseDto;
import com.portal.certificateservice.dto.CertificateVerificationResponseDto;
import com.portal.certificateservice.dto.GenerateCertificateRequestDto;
import com.portal.certificateservice.entity.Certificate;
import com.portal.certificateservice.entity.CertificateTemplate;
import com.portal.certificateservice.entity.CertificateVerificationLog;
import com.portal.certificateservice.exception.CertificateException;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.repository.CertificateRepository;
import com.portal.certificateservice.repository.CertificateTemplateRepository;
import com.portal.certificateservice.repository.CertificateVerificationLogRepository;
import com.portal.certificateservice.service.CertificateService;
import com.portal.certificateservice.service.PdfGeneratorService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private CertificateTemplateRepository templateRepository;

    @Mock
    private CertificateVerificationLogRepository verificationLogRepository;

    @Mock
    private PdfGeneratorService pdfGeneratorService;

    @InjectMocks
    private CertificateService certificateService;

    private UUID studentId;
    private UUID eventId;
    private UUID templateId;
    private CertificateTemplate template;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        templateId = UUID.randomUUID();

        template = new CertificateTemplate("Standard Workshop Template", "/path/to/tpl", "{}", true);
        template.setId(templateId);
    }

    @Test
    void testGenerateCertificate_Success() {
        GenerateCertificateRequestDto request = new GenerateCertificateRequestDto(
                studentId, eventId, templateId, "John Doe", "Java 21 Microservices Workshop"
        );

        when(certificateRepository.existsByStudentIdAndEventId(studentId, eventId)).thenReturn(false);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(pdfGeneratorService.generatePdfCertificate(any(), anyString(), anyString(), anyString(), any(), any())).thenReturn("/storage/cert.pdf");

        Certificate savedCert = new Certificate(studentId, eventId, templateId, "CERT-12345678", "/storage/cert.pdf", "ISSUED", LocalDateTime.now());
        savedCert.setId(UUID.randomUUID());
        when(certificateRepository.save(any(Certificate.class))).thenReturn(savedCert);

        CertificateResponseDto response = certificateService.generateCertificate(request);

        assertNotNull(response);
        assertEquals(studentId, response.getStudentId());
        assertEquals(eventId, response.getEventId());
        assertEquals("ISSUED", response.getStatus());
        verify(certificateRepository, times(1)).save(any(Certificate.class));
    }

    @Test
    void testGenerateCertificate_Duplicate_ThrowsException() {
        GenerateCertificateRequestDto request = new GenerateCertificateRequestDto(
                studentId, eventId, templateId, "John Doe", "Java Workshop"
        );

        when(certificateRepository.existsByStudentIdAndEventId(studentId, eventId)).thenReturn(true);

        assertThrows(CertificateException.class, () -> certificateService.generateCertificate(request));
    }

    @Test
    void testVerifyCertificate_ValidCode() {
        String code = "CERT-99887766";
        UUID certId = UUID.randomUUID();
        Certificate cert = new Certificate(studentId, eventId, templateId, code, "/pdf", "ISSUED", LocalDateTime.now());
        cert.setId(certId);

        when(certificateRepository.findByVerificationCode(code)).thenReturn(Optional.of(cert));

        CertificateVerificationResponseDto response = certificateService.verifyCertificate(code, "127.0.0.1");

        assertTrue(response.isValid());
        assertEquals(code, response.getVerificationCode());
        assertEquals("ISSUED", response.getStatus());
        verify(verificationLogRepository, times(1)).save(any(CertificateVerificationLog.class));
    }

    @Test
    void testVerifyCertificate_NotFound_ThrowsException() {
        when(certificateRepository.findByVerificationCode("INVALID-CODE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> certificateService.verifyCertificate("INVALID-CODE", "127.0.0.1"));
    }
}
