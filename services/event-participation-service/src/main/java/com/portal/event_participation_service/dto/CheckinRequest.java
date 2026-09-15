package com.portal.event_participation_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckinRequest(
        @NotNull UUID registrationId,
        @NotBlank String qrCodeValue
) {
}