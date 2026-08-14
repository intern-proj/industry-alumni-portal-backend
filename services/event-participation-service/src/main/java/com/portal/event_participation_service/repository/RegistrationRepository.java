package com.portal.event_participation_service.repository;

import com.portal.event_participation_service.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    List<Registration> findByEventId(UUID eventId);
    List<Registration> findByStudentId(UUID studentId);
}