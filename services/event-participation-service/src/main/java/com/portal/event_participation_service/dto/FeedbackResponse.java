package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.Feedback;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID feedbackId,
        UUID registrationId,
        Integer rating,
        String comments,
        Instant submittedAt
) {
    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(
                f.getFeedbackId(), f.getRegistrationId(), f.getRating(),
                f.getComments(), f.getSubmittedAt()
        );
    }
}