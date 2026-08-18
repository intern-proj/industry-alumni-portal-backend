package com.nsbm.audit_storage_service.controller;

import com.nsbm.audit_storage_service.dto.AuditLogRequest;
import com.nsbm.audit_storage_service.dto.AuditLogResponse;
import com.nsbm.audit_storage_service.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Accepts audit events from other microservices and persists them asynchronously.
     */
    @PostMapping("/log")
    public ResponseEntity<Void> logAction(@Valid @RequestBody AuditLogRequest request) {
        auditService.logActionAsync(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Paginated immutable audit trail for the System Administrator dashboard.
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogResponse>> getLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AuditLogResponse> page;

        if (userId != null && !userId.isBlank()) {
            page = auditService.getLogsByUserId(userId, pageable);
        } else if (action != null && !action.isBlank()) {
            page = auditService.getLogsByAction(action, pageable);
        } else if (from != null && to != null) {
            page = auditService.getLogsBetween(from, to, pageable);
        } else {
            page = auditService.getLogs(pageable);
        }

        return ResponseEntity.ok(page);
    }
}
