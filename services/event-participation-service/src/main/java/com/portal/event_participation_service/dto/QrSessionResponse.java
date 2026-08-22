package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.QrSession;

import java.time.Instant;
import java.util.UUID;

public record QrSessionResponse(
        UUID qrId,
        UUID eventId,
        String qrCodeValue,
        String qrLink,
        Instant generatedAt,
        Instant expiresAt,
        boolean active
) {
    public static QrSessionResponse from(QrSession qr, String baseUrl) {
        return new QrSessionResponse(
                qr.getQrId(),
                qr.getEventId(),
                qr.getQrCodeValue(),
                baseUrl + "/api/v1/qr-sessions/" + qr.getQrCodeValue() + "/verify",
                qr.getGeneratedAt(),
                qr.getExpiresAt(),
                qr.isActive() && Instant.now().isBefore(qr.getExpiresAt())
        );
    }
}
