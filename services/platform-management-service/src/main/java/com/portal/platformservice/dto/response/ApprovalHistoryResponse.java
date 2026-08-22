package com.portal.platformservice.dto.response;

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
public class ApprovalHistoryResponse {

    private String fromStatus;
    private String toStatus;
    private UUID changedByUserId;
    private Instant changedAt;
    private String remarks;
}
