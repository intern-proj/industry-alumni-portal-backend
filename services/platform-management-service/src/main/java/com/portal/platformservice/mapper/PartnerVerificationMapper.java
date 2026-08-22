package com.portal.platformservice.mapper;

import com.portal.platformservice.dto.response.PartnerDocumentResponse;
import com.portal.platformservice.dto.response.PartnerVerificationResponse;
import com.portal.platformservice.dto.response.PartnerVerificationSummaryResponse;
import com.portal.platformservice.entity.PartnerDocument;
import com.portal.platformservice.entity.PartnerVerification;

public final class PartnerVerificationMapper {

    private PartnerVerificationMapper() {
    }

    public static PartnerVerificationResponse toResponse(PartnerVerification verification) {
        return PartnerVerificationResponse.builder()
                .id(verification.getId())
                .userId(verification.getUserId())
                .organizationNameSnapshot(verification.getOrganizationNameSnapshot())
                .contactEmailSnapshot(verification.getContactEmailSnapshot())
                .status(verification.getStatus())
                .submittedAt(verification.getSubmittedAt())
                .reviewedAt(verification.getReviewedAt())
                .reviewedByUserId(verification.getReviewedByUserId())
                .decisionNotes(verification.getDecisionNotes())
                .rejectionReason(verification.getRejectionReason())
                .syncStatus(verification.getSyncStatus())
                .version(verification.getVersion())
                .documents(verification.getDocuments().stream()
                        .map(PartnerVerificationMapper::toDocumentResponse)
                        .toList())
                .build();
    }

    public static PartnerVerificationSummaryResponse toSummaryResponse(PartnerVerification verification) {
        return PartnerVerificationSummaryResponse.builder()
                .id(verification.getId())
                .userId(verification.getUserId())
                .organizationNameSnapshot(verification.getOrganizationNameSnapshot())
                .contactEmailSnapshot(verification.getContactEmailSnapshot())
                .status(verification.getStatus())
                .submittedAt(verification.getSubmittedAt())
                .build();
    }

    public static PartnerDocumentResponse toDocumentResponse(PartnerDocument document) {
        return PartnerDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .storageFileId(document.getStorageFileId())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .uploadedAt(document.getUploadedAt())
                .verified(document.isVerified())
                .build();
    }
}
