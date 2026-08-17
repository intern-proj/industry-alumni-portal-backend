package com.portal.platformservice.event;

import com.portal.platformservice.entity.VacancyApprovalStatus;

import java.util.UUID;

public record VacancyApprovalDecidedEvent(
        UUID approvalId,
        UUID vacancyId,
        VacancyApprovalStatus outcome
) {
}
