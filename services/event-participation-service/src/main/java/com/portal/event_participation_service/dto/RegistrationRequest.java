package com.portal.event_participation_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegistrationRequest(
        @NotNull UUID eventId,
        @NotNull UUID studentId
) {
}