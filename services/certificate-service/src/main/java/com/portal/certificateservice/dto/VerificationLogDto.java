package com.portal.certificateservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class VerificationLogDto {

    private UUID id;
    private UUID certificateId;
    private LocalDateTime verifiedAt;
    private String ipAddress;

    public VerificationLogDto() {
    }

    public VerificationLogDto(UUID id, UUID certificateId, LocalDateTime verifiedAt, String ipAddress) {
        this.id = id;
        this.certificateId = certificateId;
        this.verifiedAt = verifiedAt;
        this.ipAddress = ipAddress;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(UUID certificateId) {
        this.certificateId = certificateId;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
