package com.portal.event_participation_service.repository;

import com.portal.event_participation_service.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    boolean existsByRegistrationId(UUID registrationId);
}