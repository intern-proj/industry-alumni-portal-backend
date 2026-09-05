package com.portal.certificateservice.service;

import com.portal.certificateservice.dto.*;
import com.portal.certificateservice.entity.Certificate;
import com.portal.certificateservice.entity.CertificateTemplate;
import com.portal.certificateservice.entity.CertificateVerificationLog;
import com.portal.certificateservice.exception.CertificateException;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.repository.CertificateRepository;
import com.portal.certificateservice.repository.CertificateTemplateRepository;
import com.portal.certificateservice.repository.CertificateVerificationLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository templateRepository;
    private final CertificateVerificationLogRepository verificationLogRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${certificate.baseUrl:${app.backend.url:${API_GATEWAY_URL:https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io}}}")
    private String baseUrl;

    public CertificateService(CertificateRepository certificateRepository,
                              CertificateTemplateRepository templateRepository,
                              CertificateVerificationLogRepository verificationLogRepository,
                              PdfGeneratorService pdfGeneratorService,
                              NotificationEventPublisher notificationEventPublisher) {
        this.certificateRepository = certificateRepository;
        this.templateRepository = templateRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    @Transactional
    public CertificateResponseDto generateCertificate(GenerateCertificateRequestDto request) {
        if (certificateRepository.existsByStudentIdAndEventId(request.getStudentId(), request.getEventId())) {
            throw new CertificateException("Certificate has already been issued to student ID " + request.getStudentId() + " for event ID " + request.getEventId(), HttpStatus.CONFLICT);
        }

        CertificateTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found with ID: " + request.getTemplateId()));

        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new CertificateException("Selected template is inactive");
        }

        UUID certificateId = UUID.randomUUID();
        String verificationCode = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime issuedAt = LocalDateTime.now();

        String pdfPath = pdfGeneratorService.generatePdfCertificate(
                certificateId,
                verificationCode,
                request.getStudentName(),
                request.getEventTitle(),
                issuedAt,
                template.getTemplateFilePath()
        );

        Certificate certificate = new Certificate(
                request.getStudentId(),
                request.getEventId(),
                request.getTemplateId(),
                verificationCode,
                pdfPath,
                "ISSUED",
                issuedAt
        );
        certificate.setId(certificateId);

        Certificate saved = certificateRepository.save(certificate);

        // Publish RabbitMQ event to notification.exchange (routing key: notification.certificate)
        String downloadUrl = baseUrl + "/api/v1/certificates/" + saved.getId() + "/download";
        CertificateNotificationEventDto notificationEvent = new CertificateNotificationEventDto(
                saved.getId(), saved.getStudentId(), saved.getEventId(), saved.getVerificationCode(), downloadUrl, saved.getIssuedAt()
        );
        notificationEventPublisher.publishCertificateNotification(notificationEvent);

        return mapToResponseDto(saved);
    }

    @Transactional
    public List<CertificateResponseDto> bulkGenerateCertificates(BulkGenerateCertificateRequestDto request) {
        CertificateTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found with ID: " + request.getTemplateId()));

        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new CertificateException("Selected template is inactive");
        }

        List<CertificateResponseDto> issuedCertificates = new ArrayList<>();

        for (UUID studentId : request.getStudentIds()) {
            if (certificateRepository.existsByStudentIdAndEventId(studentId, request.getEventId())) {
                Certificate existing = certificateRepository.findByStudentIdAndEventId(studentId, request.getEventId()).get();
                issuedCertificates.add(mapToResponseDto(existing));
                continue;
            }

            UUID certificateId = UUID.randomUUID();
            String verificationCode = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            LocalDateTime issuedAt = LocalDateTime.now();

            String pdfPath = pdfGeneratorService.generatePdfCertificate(
                    certificateId,
                    verificationCode,
                    "Student (" + studentId.toString().substring(0, 8) + ")",
                    request.getEventTitle(),
                    issuedAt,
                    template.getTemplateFilePath()
            );

            Certificate certificate = new Certificate(
                    studentId,
                    request.getEventId(),
                    request.getTemplateId(),
                    verificationCode,
                    pdfPath,
                    "ISSUED",
                    issuedAt
            );
            certificate.setId(certificateId);

            Certificate saved = certificateRepository.save(certificate);

            // Publish RabbitMQ notification event
            String downloadUrl = baseUrl + "/api/v1/certificates/" + saved.getId() + "/download";
            CertificateNotificationEventDto notificationEvent = new CertificateNotificationEventDto(
                    saved.getId(), saved.getStudentId(), saved.getEventId(), saved.getVerificationCode(), downloadUrl, saved.getIssuedAt()
            );
            notificationEventPublisher.publishCertificateNotification(notificationEvent);

            issuedCertificates.add(mapToResponseDto(saved));
        }

        return issuedCertificates;
    }

    public List<CertificateResponseDto> getCertificatesByStudent(UUID studentId) {
        return certificateRepository.findByStudentId(studentId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<CertificateResponseDto> getCertificatesByEvent(UUID eventId) {
        return certificateRepository.findByEventId(eventId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public CertificateResponseDto getCertificateById(UUID id) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + id));
        return mapToResponseDto(cert);
    }

    @Transactional
    public CertificateResponseDto updateCertificateStatus(UUID id, String status) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + id));
        cert.setStatus(status);
        Certificate updated = certificateRepository.save(cert);
        return mapToResponseDto(updated);
    }

    @Transactional
    public CertificateVerificationResponseDto verifyCertificate(String verificationCode, String ipAddress) {
        Certificate cert = certificateRepository.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with verification code: " + verificationCode));

        CertificateVerificationLog log = new CertificateVerificationLog(cert.getId(), ipAddress != null ? ipAddress : "UNKNOWN");
        verificationLogRepository.save(log);

        boolean isValid = "ISSUED".equalsIgnoreCase(cert.getStatus());
        String message = isValid ? "Certificate is authentic and valid." : "Certificate is status: " + cert.getStatus();

        return new CertificateVerificationResponseDto(
                isValid,
                cert.getId(),
                cert.getVerificationCode(),
                cert.getStudentId(),
                cert.getEventId(),
                cert.getTemplateId(),
                cert.getStatus(),
                cert.getIssuedAt(),
                message
        );
    }

    public List<VerificationLogDto> getVerificationLogs(UUID certificateId) {
        if (!certificateRepository.existsById(certificateId)) {
            throw new ResourceNotFoundException("Certificate not found with ID: " + certificateId);
        }
        return verificationLogRepository.findByCertificateIdOrderByVerifiedAtDesc(certificateId).stream()
                .map(log -> new VerificationLogDto(log.getId(), log.getCertificateId(), log.getVerifiedAt(), log.getIpAddress()))
                .collect(Collectors.toList());
    }

    public CertificateStatsDto getStats() {
        long totalIssued = certificateRepository.count();
        long totalActiveTemplates = templateRepository.findByIsActiveTrue().size();
        long totalVerifications = verificationLogRepository.count();
        return new CertificateStatsDto(totalIssued, totalActiveTemplates, totalVerifications);
    }

    public File getCertificatePdfFile(UUID id) {
        Certificate cert = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + id));

        if (cert.getPdfFilePath() == null || cert.getPdfFilePath().isBlank()) {
            throw new ResourceNotFoundException("PDF file path is not set for certificate ID: " + id);
        }

        File file = new File(cert.getPdfFilePath());
        if (!file.exists()) {
            String newPdfPath = pdfGeneratorService.generatePdfCertificate(
                    cert.getId(),
                    cert.getVerificationCode(),
                    "Student",
                    "Event",
                    cert.getIssuedAt()
            );
            cert.setPdfFilePath(newPdfPath);
            certificateRepository.save(cert);
            file = new File(newPdfPath);
        }
        return file;
    }

    private CertificateResponseDto mapToResponseDto(Certificate cert) {
        return new CertificateResponseDto(
                cert.getId(),
                cert.getStudentId(),
                cert.getEventId(),
                cert.getTemplateId(),
                cert.getVerificationCode(),
                cert.getPdfFilePath(),
                cert.getStatus(),
                cert.getIssuedAt()
        );
    }
}
