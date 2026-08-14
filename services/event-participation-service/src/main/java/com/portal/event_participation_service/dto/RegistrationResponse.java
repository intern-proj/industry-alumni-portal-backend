package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.Registration;
import java.time.Instant;
import java.util.UUID;

public record RegistrationResponse(
        UUID registrationId,
        UUID eventId,
        UUID studentId,
        String status,
        Instant registeredAt
) {
    public static RegistrationResponse from(Registration r) {
        return new RegistrationResponse(
                r.getRegistrationId(), r.getEventId(), r.getStudentId(),
                r.getStatus().name(), r.getRegisteredAt()
        );
    }
}
