package com.portal.event_participation_service.dto;

import java.time.Instant;
import java.util.UUID;

public record QrVerifyResponse(
        UUID eventId,
        boolean active,
        Instant expiresAt
) {
}