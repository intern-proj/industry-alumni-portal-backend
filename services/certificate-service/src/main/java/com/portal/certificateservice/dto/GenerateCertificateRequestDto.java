package com.portal.certificateservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class GenerateCertificateRequestDto {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    private String studentName;
    private String eventTitle;

    public GenerateCertificateRequestDto() {
    }

    public GenerateCertificateRequestDto(UUID studentId, UUID eventId, UUID templateId, String studentName, String eventTitle) {
        this.studentId = studentId;
        this.eventId = eventId;
        this.templateId = templateId;
        this.studentName = studentName;
        this.eventTitle = eventTitle;
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }
}
