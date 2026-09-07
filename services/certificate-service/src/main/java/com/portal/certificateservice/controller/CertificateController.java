package com.portal.certificateservice.controller;

import com.portal.certificateservice.dto.*;
import com.portal.certificateservice.service.CertificateService;
import com.portal.certificateservice.service.CertificateTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateTemplateService templateService;

    public CertificateController(CertificateService certificateService, CertificateTemplateService templateService) {
        this.certificateService = certificateService;
        this.templateService = templateService;
    }

    /**
     * POST /api/v1/certificates/templates
     * Upload PDF certificate background template
     */
    @PostMapping("/templates")
    public ResponseEntity<TemplateResponseDto> uploadTemplate(@Valid @RequestBody CreateTemplateRequestDto request) {
        TemplateResponseDto created = templateService.createTemplate(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/certificates/templates
     * List certificate background templates
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TemplateResponseDto>> getAllTemplates(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<TemplateResponseDto> templates = activeOnly ? templateService.getActiveTemplates() : templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * POST /api/v1/certificates/generate
     * Batch / Single generate PDF certificates for eligible attendees
     */
    @PostMapping("/generate")
    public ResponseEntity<CertificateResponseDto> generateCertificate(@Valid @RequestBody GenerateCertificateRequestDto request) {
        CertificateResponseDto created = certificateService.generateCertificate(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * POST /api/v1/certificates/batch-generate
     */
    @PostMapping("/batch-generate")
    public ResponseEntity<List<CertificateResponseDto>> bulkGenerateCertificates(@Valid @RequestBody BulkGenerateCertificateRequestDto request) {
        List<CertificateResponseDto> createdList = certificateService.bulkGenerateCertificates(request);
        return new ResponseEntity<>(createdList, HttpStatus.CREATED);
    }

    /**
     * GET /api/v1/certificates/student/{studentId}
     * Fetch earned certificates for a student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CertificateResponseDto>> getStudentCertificates(@PathVariable UUID studentId) {
        return ResponseEntity.ok(certificateService.getCertificatesByStudent(studentId));
    }

    /**
     * GET /api/v1/certificates/verify/{qrHash}
     * Public QR verification lookup for third parties
     */
    @GetMapping("/verify/{qrHash}")
    public ResponseEntity<CertificateVerificationResponseDto> verifyCertificate(@PathVariable("qrHash") String qrHash, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        return ResponseEntity.ok(certificateService.verifyCertificate(qrHash, clientIp));
    }

    /**
     * GET /api/v1/certificates/{id}/download
     * Download rendered PDF certificate file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadCertificatePdf(@PathVariable UUID id) {
        File file = certificateService.getCertificatePdfFile(id);
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponseDto> getCertificateById(@PathVariable UUID id) {
        return ResponseEntity.ok(certificateService.getCertificateById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CertificateResponseDto> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(certificateService.updateCertificateStatus(id, status));
    }

    @GetMapping("/{id}/verification-logs")
    public ResponseEntity<List<VerificationLogDto>> getVerificationLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(certificateService.getVerificationLogs(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<CertificateStatsDto> getStats() {
        return ResponseEntity.ok(certificateService.getStats());
    }
}
