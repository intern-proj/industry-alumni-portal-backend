package com.nsbm.application_service.repository;

import com.nsbm.application_service.model.ApplicationStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationStatusAuditRepository extends JpaRepository<ApplicationStatusAudit, UUID> {
    List<ApplicationStatusAudit> findByApplicationIdOrderByChangedAtDesc(UUID applicationId);
}
