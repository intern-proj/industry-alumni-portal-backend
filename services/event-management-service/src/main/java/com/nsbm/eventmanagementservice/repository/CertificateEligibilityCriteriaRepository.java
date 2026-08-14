package com.nsbm.eventmanagementservice.repository;
import com.nsbm.eventmanagementservice.model.CertificateEligibilityCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateEligibilityCriteriaRepository extends JpaRepository<CertificateEligibilityCriteria, Long> {
    Optional<CertificateEligibilityCriteria> findByEventId(Long eventId);
}
