package com.nsbm.audit_storage_service.service;

import com.nsbm.audit_storage_service.dto.AuditLogRequest;
import com.nsbm.audit_storage_service.dto.AuditLogResponse;
import com.nsbm.audit_storage_service.model.AuditLog;
import com.nsbm.audit_storage_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async("auditTaskExecutor")
    @Transactional
    public CompletableFuture<AuditLogResponse> logActionAsync(AuditLogRequest request) {
        AuditLogResponse response = persist(request);
        log.debug("Async audit log saved: userId={}, action={}", response.getUserId(), response.getAction());
        return CompletableFuture.completedFuture(response);
    }

    @Override
    @Transactional
    public AuditLogResponse logAction(AuditLogRequest request) {
        return persist(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByUserId(String userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository
                .findByActionContainingIgnoreCaseOrderByTimestampDesc(action, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsBetween(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository
                .findByTimestampBetweenOrderByTimestampDesc(from, to, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse persist(AuditLogRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .userId(request.getUserId())
                .action(request.getAction())
                .ipAddress(request.getIpAddress())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .details(request.getDetails())
                .timestamp(Instant.now())
                .build();

        return toResponse(auditLogRepository.save(auditLog));
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .action(auditLog.getAction())
                .ipAddress(auditLog.getIpAddress())
                .timestamp(auditLog.getTimestamp())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .details(auditLog.getDetails())
                .build();
    }
}
