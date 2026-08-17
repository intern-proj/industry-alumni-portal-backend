package com.portal.platformservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyApprovalDecisionRequest {

    @NotNull
    private VacancyApprovalDecision decision;

    private String decisionNotes;

    private String rejectionReason;

    @NotNull
    private UUID actingUserId;
}
