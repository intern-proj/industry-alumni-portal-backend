package com.portal.platformservice.mapper;

import com.portal.platformservice.dto.response.VacancyApprovalResponse;
import com.portal.platformservice.dto.response.VacancyApprovalSummaryResponse;
import com.portal.platformservice.entity.VacancyApproval;

public final class VacancyApprovalMapper {

    private VacancyApprovalMapper() {
    }

    public static VacancyApprovalResponse toResponse(VacancyApproval approval) {
        return VacancyApprovalResponse.builder()
                .id(approval.getId())
                .vacancyId(approval.getVacancyId())
                .companyUserId(approval.getCompanyUserId())
                .submittedByUserId(approval.getSubmittedByUserId())
                .vacancyTitleSnapshot(approval.getVacancyTitleSnapshot())
                .companyNameSnapshot(approval.getCompanyNameSnapshot())
                .status(approval.getStatus())
                .submittedAt(approval.getSubmittedAt())
                .assignedReviewerId(approval.getAssignedReviewerId())
                .reviewedAt(approval.getReviewedAt())
                .reviewedByUserId(approval.getReviewedByUserId())
                .decisionNotes(approval.getDecisionNotes())
                .rejectionReason(approval.getRejectionReason())
                .syncStatus(approval.getSyncStatus())
                .version(approval.getVersion())
                .build();
    }

    public static VacancyApprovalSummaryResponse toSummaryResponse(VacancyApproval approval) {
        return VacancyApprovalSummaryResponse.builder()
                .id(approval.getId())
                .vacancyId(approval.getVacancyId())
                .companyUserId(approval.getCompanyUserId())
                .vacancyTitleSnapshot(approval.getVacancyTitleSnapshot())
                .companyNameSnapshot(approval.getCompanyNameSnapshot())
                .status(approval.getStatus())
                .submittedAt(approval.getSubmittedAt())
                .build();
    }
}
