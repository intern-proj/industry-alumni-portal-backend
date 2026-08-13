package com.portal.event_participation_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "registration_id", updatable = false, nullable = false)
    private UUID registrationId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId; // external ref — owned by Event Management Service

    @Column(name = "student_id", nullable = false)
    private UUID studentId; // external ref — owned by Student Profile Service

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status;

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false, nullable = false)
    private Instant registeredAt;

    public enum RegistrationStatus {
        PENDING, APPROVED, REJECTED
    }
}
