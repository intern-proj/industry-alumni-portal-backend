package com.portal.event_participation_service.service;

import com.portal.event_participation_service.dto.FeedbackRequest;
import com.portal.event_participation_service.dto.FeedbackResponse;
import com.portal.event_participation_service.entity.Feedback;
import com.portal.event_participation_service.exception.ResourceNotFoundException;
import com.portal.event_participation_service.repository.AttendanceRepository;
import com.portal.event_participation_service.repository.FeedbackRepository;
import com.portal.event_participation_service.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final CertificateEligibilityService certificateEligibilityService;

    public FeedbackResponse submit(FeedbackRequest request) {
        // 1. Confirm registration exists
        if (!registrationRepository.existsById(request.registrationId())) {
            throw new ResourceNotFoundException(
                    "Registration with ID " + request.registrationId() + " was not found.");
        }

        // 2. Must have actually checked in before leaving feedback
        if (!attendanceRepository.existsByRegistrationId(request.registrationId())) {
            throw new IllegalStateException(
                    "Feedback can only be submitted after checking in to the event.");
        }

        // 3. One feedback per registration
        if (feedbackRepository.existsByRegistrationId(request.registrationId())) {
            throw new IllegalStateException(
                    "Feedback has already been submitted for registration " + request.registrationId());
        }

        Feedback feedback = Feedback.builder()
                .registrationId(request.registrationId())
                .rating(request.rating())
                .comments(request.comments())
                .build();

        FeedbackResponse response = FeedbackResponse.from(feedbackRepository.save(feedback));

        // Trigger certificate eligibility evaluation now that feedback is in
        certificateEligibilityService.evaluate(request.registrationId());

        return response;
    }
}
