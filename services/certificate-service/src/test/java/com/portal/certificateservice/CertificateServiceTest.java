package com.portal.certificateservice;

import com.portal.certificateservice.dto.*;
import com.portal.certificateservice.entity.Certificate;
import com.portal.certificateservice.entity.CertificateTemplate;
import com.portal.certificateservice.entity.CertificateVerificationLog;
import com.portal.certificateservice.exception.CertificateException;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.repository.CertificateRepository;
import com.portal.certificateservice.repository.CertificateTemplateRepository;
import com.portal.certificateservice.repository.CertificateVerificationLogRepository;
import com.portal.certificateservice.service.CertificateService;
import com.portal.certificateservice.service.NotificationEventPublisher;
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
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private CertificateService certificateService;

    private UUID studentId;
    private UUID eventId;
    private UUID templateId;
    private UUID certId;
    private CertificateTemplate activeTemplate;
    private Certificate certificate;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        certId = UUID.randomUUID();

        activeTemplate = new CertificateTemplate("Standard Template", "/templates/bg.png", "{}", true);
        activeTemplate.setId(templateId);

        certificate = new Certificate(studentId, eventId, templateId, "CERT-12345678", "/storage/cert.pdf", "ISSUED", LocalDateTime.now());
        certificate.setId(certId);
    }

    @Test
    void testGenerateCertificate_Success() {
        GenerateCertificateRequestDto request = new GenerateCertificateRequestDto(studentId, eventId, templateId, "Alice", "Tech Talk");

        when(certificateRepository.existsByStudentIdAndEventId(studentId, eventId)).thenReturn(false);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(activeTemplate));
        when(pdfGeneratorService.generatePdfCertificate(any(), any(), any(), any(), any(), any())).thenReturn("/storage/cert.pdf");
        when(certificateRepository.save(any(Certificate.class))).thenReturn(certificate);

        CertificateResponseDto response = certificateService.generateCertificate(request);

        assertNotNull(response);
        assertEquals("CERT-12345678", response.getVerificationCode());
        verify(notificationEventPublisher, times(1)).publishCertificateNotification(any());
    }

    @Test
    void testGenerateCertificate_AlreadyIssued_ThrowsException() {
        GenerateCertificateRequestDto request = new GenerateCertificateRequestDto(studentId, eventId, templateId, "Alice", "Tech Talk");

        when(certificateRepository.existsByStudentIdAndEventId(studentId, eventId)).thenReturn(true);

        assertThrows(CertificateException.class, () -> certificateService.generateCertificate(request));
    }

    @Test
    void testVerifyCertificate_Success() {
        when(certificateRepository.findByVerificationCode("CERT-12345678")).thenReturn(Optional.of(certificate));

        CertificateVerificationResponseDto response = certificateService.verifyCertificate("CERT-12345678", "192.168.1.1");

        assertTrue(response.isValid());
        assertEquals("CERT-12345678", response.getVerificationCode());
        verify(verificationLogRepository, times(1)).save(any(CertificateVerificationLog.class));
    }

    @Test
    void testVerifyCertificate_NotFound_ThrowsException() {
        when(certificateRepository.findByVerificationCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> certificateService.verifyCertificate("INVALID", "127.0.0.1"));
    }

    @Test
    void testGetCertificatesByStudent_Success() {
        when(certificateRepository.findByStudentId(studentId)).thenReturn(List.of(certificate));

        List<CertificateResponseDto> list = certificateService.getCertificatesByStudent(studentId);

        assertFalse(list.isEmpty());
        assertEquals(studentId, list.get(0).getStudentId());
    }

    @Test
    void testGetCertificateById_Success() {
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));

        CertificateResponseDto dto = certificateService.getCertificateById(certId);

        assertNotNull(dto);
        assertEquals(certId, dto.getId());
    }

    @Test
    void testUpdateCertificateStatus_Success() {
        when(certificateRepository.findById(certId)).thenReturn(Optional.of(certificate));
        when(certificateRepository.save(any())).thenReturn(certificate);

        CertificateResponseDto dto = certificateService.updateCertificateStatus(certId, "REVOKED");

        assertNotNull(dto);
        verify(certificateRepository).save(certificate);
    }

    @Test
    void testGetStats_Success() {
        when(certificateRepository.count()).thenReturn(100L);
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(activeTemplate));
        when(verificationLogRepository.count()).thenReturn(50L);

        CertificateStatsDto stats = certificateService.getStats();

        assertEquals(100L, stats.getTotalCertificatesIssued());
        assertEquals(1L, stats.getTotalActiveTemplates());
        assertEquals(50L, stats.getTotalVerificationsCount());
    }
}
