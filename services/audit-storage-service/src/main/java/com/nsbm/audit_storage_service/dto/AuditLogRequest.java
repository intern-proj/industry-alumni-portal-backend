package com.nsbm.audit_storage_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogRequest {

    @NotBlank(message = "userId is required")
    @Size(max = 100)
    private String userId;

    @NotBlank(message = "action is required")
    @Size(max = 255)
    private String action;

    @NotBlank(message = "ipAddress is required")
    @Size(max = 45)
    private String ipAddress;

    @Size(max = 100)
    private String resourceType;

    @Size(max = 100)
    private String resourceId;

    private String details;
}
