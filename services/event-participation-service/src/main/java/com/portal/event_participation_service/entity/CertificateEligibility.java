package com.portal.event_participation_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificate_eligibility")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "eligibility_id", updatable = false, nullable = false)
    private UUID eligibilityId;

    @Column(name = "registration_id", nullable = false, unique = true)
    private UUID registrationId;

    @Column(name = "attendance_met", nullable = false)
    private boolean attendanceMet;

    @Column(name = "feedback_met", nullable = false)
    private boolean feedbackMet;

    @Column(name = "is_eligible", nullable = false)
    private boolean eligible;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;
}