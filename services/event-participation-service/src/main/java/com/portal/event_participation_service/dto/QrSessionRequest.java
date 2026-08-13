package com.portal.event_participation_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QrSessionRequest(
        @NotNull @Min(1) Integer validForMinutes
) {
}
