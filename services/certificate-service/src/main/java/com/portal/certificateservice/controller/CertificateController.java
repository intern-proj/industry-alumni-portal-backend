package com.portal.certificateservice.controller;

import com.portal.certificateservice.dto.*;
import com.portal.certificateservice.service.CertificateService;
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

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/generate")
    public ResponseEntity<CertificateResponseDto> generateCertificate(@Valid @RequestBody GenerateCertificateRequestDto request) {
        CertificateResponseDto created = certificateService.generateCertificate(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/bulk-generate")
    public ResponseEntity<List<CertificateResponseDto>> bulkGenerateCertificates(@Valid @RequestBody BulkGenerateCertificateRequestDto request) {
        List<CertificateResponseDto> createdList = certificateService.bulkGenerateCertificates(request);
        return new ResponseEntity<>(createdList, HttpStatus.CREATED);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CertificateResponseDto>> getStudentCertificates(@PathVariable UUID studentId) {
        return ResponseEntity.ok(certificateService.getCertificatesByStudent(studentId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<CertificateResponseDto>> getEventCertificates(@PathVariable UUID eventId) {
        return ResponseEntity.ok(certificateService.getCertificatesByEvent(eventId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponseDto> getCertificateById(@PathVariable UUID id) {
        return ResponseEntity.ok(certificateService.getCertificateById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadCertificatePdf(@PathVariable UUID id) {
        File file = certificateService.getCertificatePdfFile(id);
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CertificateResponseDto> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(certificateService.updateCertificateStatus(id, status));
    }

    @GetMapping("/verify/{code}")
    public ResponseEntity<CertificateVerificationResponseDto> verifyCertificate(@PathVariable("code") String code, HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }
        return ResponseEntity.ok(certificateService.verifyCertificate(code, clientIp));
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