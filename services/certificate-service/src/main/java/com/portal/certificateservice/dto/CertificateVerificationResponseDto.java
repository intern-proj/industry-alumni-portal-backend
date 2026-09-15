package com.portal.certificateservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CertificateVerificationResponseDto {

    private boolean isValid;
    private UUID certificateId;
    private String verificationCode;
    private UUID studentId;
    private UUID eventId;
    private UUID templateId;
    private String status;
    private LocalDateTime issuedAt;
    private String message;

    public CertificateVerificationResponseDto() {
    }

    public CertificateVerificationResponseDto(boolean isValid, UUID certificateId, String verificationCode, UUID studentId, UUID eventId, UUID templateId, String status, LocalDateTime issuedAt, String message) {
        this.isValid = isValid;
        this.certificateId = certificateId;
        this.verificationCode = verificationCode;
        this.studentId = studentId;
        this.eventId = eventId;
        this.templateId = templateId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.message = message;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public UUID getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(UUID certificateId) {
        this.certificateId = certificateId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
