package com.nsbm.audit_storage_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private UUID id;
    private String userId;
    private String action;
    private String ipAddress;
    private Instant timestamp;
    private String resourceType;
    private String resourceId;
    private String details;
}
