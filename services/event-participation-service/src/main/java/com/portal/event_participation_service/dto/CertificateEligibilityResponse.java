package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.CertificateEligibility;

import java.time.Instant;
import java.util.UUID;

public record CertificateEligibilityResponse(
        UUID eligibilityId,
        UUID registrationId,
        boolean attendanceMet,
        boolean feedbackMet,
        boolean eligible,
        Instant evaluatedAt
) {
    public static CertificateEligibilityResponse from(CertificateEligibility e) {
        return new CertificateEligibilityResponse(
                e.getEligibilityId(), e.getRegistrationId(), e.isAttendanceMet(),
                e.isFeedbackMet(), e.isEligible(), e.getEvaluatedAt()
        );
    }
}
