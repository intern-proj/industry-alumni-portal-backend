package com.portal.platformservice.dto.request;

import jakarta.validation.constraints.Email;
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
 * changes via submit-for-review/claim/decide, so every status change is
 * always captured by the state machine and its history entry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerVerificationAdminEditRequest {

    private String organizationNameSnapshot;

    @Email
    private String contactEmailSnapshot;

    private String decisionNotes;

    private String rejectionReason;

    private UUID reviewedByUserId;

    @NotNull
    private UUID actingUserId;
}
