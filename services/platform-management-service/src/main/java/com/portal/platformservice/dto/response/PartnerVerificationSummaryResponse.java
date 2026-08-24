package com.portal.platformservice.dto.response;

import com.portal.platformservice.entity.VerificationStatus;
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
public class PartnerVerificationSummaryResponse {

    private UUID id;
    private UUID userId;
    private String organizationNameSnapshot;
    private String contactEmailSnapshot;
    private VerificationStatus status;
    private Instant submittedAt;
}
