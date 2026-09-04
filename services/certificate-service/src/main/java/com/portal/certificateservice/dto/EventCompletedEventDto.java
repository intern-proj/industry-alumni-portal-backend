package com.portal.certificateservice.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class EventCompletedEventDto implements Serializable {

    private UUID eventId;
    private String eventTitle;
    private UUID templateId;
    private List<UUID> eligibleStudentIds;

    public EventCompletedEventDto() {
    }

    public EventCompletedEventDto(UUID eventId, String eventTitle, UUID templateId, List<UUID> eligibleStudentIds) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.templateId = templateId;
        this.eligibleStudentIds = eligibleStudentIds;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public List<UUID> getEligibleStudentIds() {
        return eligibleStudentIds;
    }

    public void setEligibleStudentIds(List<UUID> eligibleStudentIds) {
        this.eligibleStudentIds = eligibleStudentIds;
    }
}
