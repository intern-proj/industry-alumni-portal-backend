package com.portal.event_participation_service.service;

import com.portal.event_participation_service.dto.CertificateEligibilityResponse;
import com.portal.event_participation_service.entity.CertificateEligibility;
import com.portal.event_participation_service.exception.ResourceNotFoundException;
import com.portal.event_participation_service.repository.AttendanceRepository;
import com.portal.event_participation_service.repository.CertificateEligibilityRepository;
import com.portal.event_participation_service.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateEligibilityService {

    private final CertificateEligibilityRepository eligibilityRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * Evaluates and persists eligibility for a registration.
     * Called automatically after feedback submission, and safe to call again
     * later (e.g. if attendance rules change) since it upserts the single record.
     */
    public CertificateEligibilityResponse evaluate(UUID registrationId) {
        boolean attendanceMet = attendanceRepository.existsByRegistrationId(registrationId);
        boolean feedbackMet = feedbackRepository.existsByRegistrationId(registrationId);
        boolean eligible = attendanceMet && feedbackMet;

        CertificateEligibility record = eligibilityRepository.findByRegistrationId(registrationId)
                .orElse(CertificateEligibility.builder()
                        .registrationId(registrationId)
                        .build());

        record.setAttendanceMet(attendanceMet);
        record.setFeedbackMet(feedbackMet);
        record.setEligible(eligible);
        record.setEvaluatedAt(Instant.now());

        return CertificateEligibilityResponse.from(eligibilityRepository.save(record));
    }

    public CertificateEligibilityResponse getByRegistration(UUID registrationId) {
        CertificateEligibility record = eligibilityRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No eligibility record found for registration " + registrationId));
        return CertificateEligibilityResponse.from(record);
    }
}