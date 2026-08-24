package com.portal.event_participation_service.repository;

import com.portal.event_participation_service.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByRegistrationId(UUID registrationId);
    boolean existsByRegistrationId(UUID registrationId);
}