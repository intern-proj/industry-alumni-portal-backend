package com.portal.certificateservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    @Column(name = "verification_code", nullable = false, unique = true)
    private String verificationCode;

    @Column(name = "status", nullable = false)
    private String status = "ISSUED";

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    public Certificate() {
    }

    public Certificate(UUID studentId, UUID eventId, UUID templateId, String verificationCode, String pdfFilePath, String status, LocalDateTime issuedAt) {
        this.studentId = studentId;
        this.eventId = eventId;
        this.templateId = templateId;
        this.verificationCode = verificationCode;
        this.pdfFilePath = pdfFilePath;
        this.status = status != null ? status : "ISSUED";
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
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

    public String getPdfFilePath() {
        return pdfFilePath;
    }

    public void setPdfFilePath(String pdfFilePath) {
        this.pdfFilePath = pdfFilePath;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
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