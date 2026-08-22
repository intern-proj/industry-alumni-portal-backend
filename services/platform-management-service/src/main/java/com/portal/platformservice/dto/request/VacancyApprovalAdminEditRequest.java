package com.portal.platformservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Partial update for administrative fields only. Any field left null is
 * left unchanged. Status is deliberately not editable here — it only
 * changes via claim/decide, so every status change is always captured by
 * the state machine and its history entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyApprovalAdminEditRequest {

    private String vacancyTitleSnapshot;

    private String companyNameSnapshot;

    private String decisionNotes;

    private String rejectionReason;

    private UUID assignedReviewerId;

    @NotNull
    private UUID actingUserId;
}
