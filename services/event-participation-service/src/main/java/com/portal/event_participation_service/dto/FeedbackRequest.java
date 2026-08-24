package com.portal.event_participation_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record FeedbackRequest(
        @NotNull UUID registrationId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 1000) String comments
) {
}
