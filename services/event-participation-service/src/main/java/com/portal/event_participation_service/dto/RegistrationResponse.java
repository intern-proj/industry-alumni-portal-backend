package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.Registration;
import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(
        UUID registrationId,
        String eventId,
        String studentId,
        String eventTitle,
        String venueName,
        String status,
        Instant registeredAt
) {
    public static RegistrationResponse from(Registration r) {
        return new RegistrationResponse(
                r.getRegistrationId(),
                r.getEventId(),
                r.getStudentId(),
                r.getEventTitle(),
                r.getVenueName(),
                r.getStatus() != null ? r.getStatus().name() : "REGISTERED",
                r.getRegisteredAt()
        );
    }
}
