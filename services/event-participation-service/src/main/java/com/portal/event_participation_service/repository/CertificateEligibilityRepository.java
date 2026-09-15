package com.portal.event_participation_service.repository;

import com.portal.event_participation_service.entity.CertificateEligibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateEligibilityRepository extends JpaRepository<CertificateEligibility, UUID> {
    Optional<CertificateEligibility> findByRegistrationId(UUID registrationId);
}