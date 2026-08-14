package com.portal.event_participation_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qr_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "qr_id", updatable = false, nullable = false)
    private UUID qrId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId; // external ref — Event Management Service

    @Column(name = "qr_code_value", nullable = false, unique = true, length = 64)
    private String qrCodeValue;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}