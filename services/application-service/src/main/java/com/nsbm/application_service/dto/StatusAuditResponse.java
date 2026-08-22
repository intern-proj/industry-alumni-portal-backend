package com.nsbm.application_service.dto;

import com.nsbm.application_service.model.ApplicationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusAuditResponse {
    private UUID id;
    private UUID applicationId;
    private ApplicationStatus previousStatus;
    private ApplicationStatus newStatus;
    private String changedBy;
    private String changeReason;
    private LocalDateTime changedAt;
}
