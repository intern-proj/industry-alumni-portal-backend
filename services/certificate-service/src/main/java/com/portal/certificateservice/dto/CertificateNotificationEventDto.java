package com.portal.certificateservice.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class CertificateNotificationEventDto implements Serializable {

    private UUID certificateId;
    private UUID studentId;
    private UUID eventId;
    private String verificationCode;
    private String downloadUrl;
    private LocalDateTime issuedAt;

    public CertificateNotificationEventDto() {
    }

    public CertificateNotificationEventDto(UUID certificateId, UUID studentId, UUID eventId, String verificationCode, String downloadUrl, LocalDateTime issuedAt) {
        this.certificateId = certificateId;
        this.studentId = studentId;
        this.eventId = eventId;
        this.verificationCode = verificationCode;
        this.downloadUrl = downloadUrl;
        this.issuedAt = issuedAt;
    }

    public UUID getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(UUID certificateId) {
        this.certificateId = certificateId;
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

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}
