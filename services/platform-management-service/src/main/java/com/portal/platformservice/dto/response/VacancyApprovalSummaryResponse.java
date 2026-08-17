package com.portal.platformservice.dto.response;

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
public class VacancyApprovalSummaryResponse {

    private UUID id;
    private UUID vacancyId;
    private UUID companyUserId;
    private String vacancyTitleSnapshot;
    private String companyNameSnapshot;
    private VacancyApprovalStatus status;
    private Instant submittedAt;
}
