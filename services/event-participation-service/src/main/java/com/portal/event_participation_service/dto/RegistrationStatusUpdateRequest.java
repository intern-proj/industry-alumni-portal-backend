package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.Registration;
import jakarta.validation.constraints.NotNull;

public record RegistrationStatusUpdateRequest(
        @NotNull Registration.RegistrationStatus status
) {
}