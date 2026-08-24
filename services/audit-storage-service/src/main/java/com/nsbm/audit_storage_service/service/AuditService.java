package com.nsbm.audit_storage_service.service;

import com.nsbm.audit_storage_service.dto.AuditLogRequest;
import com.nsbm.audit_storage_service.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface AuditService {

    /**
     * Persists an audit log asynchronously so calling microservices are not blocked.
     */
    CompletableFuture<AuditLogResponse> logActionAsync(AuditLogRequest request);

    /**
     * Synchronous variant used when the caller needs the persisted record immediately.
     */
    AuditLogResponse logAction(AuditLogRequest request);

    Page<AuditLogResponse> getLogs(Pageable pageable);

    Page<AuditLogResponse> getLogsByUserId(String userId, Pageable pageable);

    Page<AuditLogResponse> getLogsByAction(String action, Pageable pageable);

    Page<AuditLogResponse> getLogsBetween(Instant from, Instant to, Pageable pageable);
}
