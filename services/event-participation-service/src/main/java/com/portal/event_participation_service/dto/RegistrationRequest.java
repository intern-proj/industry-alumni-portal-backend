package com.portal.event_participation_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
        @NotBlank String eventId,
        @NotBlank String studentId,
        String eventTitle,
        String venueName
) {
}