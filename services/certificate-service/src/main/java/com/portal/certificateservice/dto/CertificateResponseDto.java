package com.portal.certificateservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CertificateResponseDto {

    private UUID id;
    private UUID studentId;
    private UUID eventId;
    private UUID templateId;
    private String verificationCode;
    private String pdfFilePath;
    private String status;
    private LocalDateTime issuedAt;

    public CertificateResponseDto() {
    }

    public CertificateResponseDto(UUID id, UUID studentId, UUID eventId, UUID templateId, String verificationCode, String pdfFilePath, String status, LocalDateTime issuedAt) {
        this.id = id;
        this.studentId = studentId;
        this.eventId = eventId;
        this.templateId = templateId;
        this.verificationCode = verificationCode;
        this.pdfFilePath = pdfFilePath;
        this.status = status;
        this.issuedAt = issuedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getPdfFilePath() {
        return pdfFilePath;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}
