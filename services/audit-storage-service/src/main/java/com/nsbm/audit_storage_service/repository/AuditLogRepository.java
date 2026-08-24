package com.nsbm.audit_storage_service.repository;

import com.nsbm.audit_storage_service.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Append-only repository: intentionally does not extend CrudRepository / JpaRepository
 * so that update and delete operations are not exposed.
 */
public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<AuditLog> findByActionContainingIgnoreCaseOrderByTimestampDesc(String action, Pageable pageable);

    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            Instant from,
            Instant to,
            Pageable pageable
    );
}
