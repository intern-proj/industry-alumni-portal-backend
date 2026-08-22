package com.portal.platformservice.dto.response;

import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VacancyApprovalStatus;
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
public class VacancyApprovalResponse {

    private UUID id;
    private UUID vacancyId;
    private UUID companyUserId;
    private UUID submittedByUserId;
    private String vacancyTitleSnapshot;
    private String companyNameSnapshot;
    private VacancyApprovalStatus status;
    private Instant submittedAt;
    private UUID assignedReviewerId;
    private Instant reviewedAt;
    private UUID reviewedByUserId;
    private String decisionNotes;
    private String rejectionReason;
    private SyncStatus syncStatus;
    private Long version;
}
