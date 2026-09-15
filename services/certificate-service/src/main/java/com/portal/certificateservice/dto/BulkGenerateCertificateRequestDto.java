package com.portal.certificateservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class BulkGenerateCertificateRequestDto {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotEmpty(message = "Student IDs list cannot be empty")
    private List<UUID> studentIds;

    private String eventTitle;

    public BulkGenerateCertificateRequestDto() {
    }

    public BulkGenerateCertificateRequestDto(UUID eventId, UUID templateId, List<UUID> studentIds, String eventTitle) {
        this.eventId = eventId;
        this.templateId = templateId;
        this.studentIds = studentIds;
        this.eventTitle = eventTitle;
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

    public List<UUID> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<UUID> studentIds) {
        this.studentIds = studentIds;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }
}
