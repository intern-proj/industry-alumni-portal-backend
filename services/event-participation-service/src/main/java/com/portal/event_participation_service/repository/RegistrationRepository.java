package com.portal.event_participation_service.repository;

import com.portal.event_participation_service.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    List<Registration> findByEventId(String eventId);
    List<Registration> findByStudentId(String studentId);
    Optional<Registration> findByEventIdAndStudentId(String eventId, String studentId);
}