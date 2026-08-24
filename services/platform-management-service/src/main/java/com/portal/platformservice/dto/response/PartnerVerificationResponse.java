package com.portal.platformservice.dto.response;

import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerVerificationResponse {

    private UUID id;
    private UUID userId;
    private String organizationNameSnapshot;
    private String contactEmailSnapshot;
    private VerificationStatus status;
    private Instant submittedAt;
    private Instant reviewedAt;
    private UUID reviewedByUserId;
    private String decisionNotes;
    private String rejectionReason;
    private SyncStatus syncStatus;
    private Long version;
    private List<PartnerDocumentResponse> documents;
}
