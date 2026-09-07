package com.portal.platformservice.service;

import com.portal.platformservice.dto.request.PartnerVerificationAdminEditRequest;
import com.portal.platformservice.dto.request.PartnerVerificationDecisionRequest;
import com.portal.platformservice.dto.request.PartnerVerificationSubmitRequest;
import com.portal.platformservice.dto.response.PartnerVerificationResponse;
import com.portal.platformservice.dto.response.PartnerVerificationSummaryResponse;
import com.portal.platformservice.entity.ApprovalType;
import com.portal.platformservice.entity.PartnerVerification;
import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VerificationStatus;
import com.portal.platformservice.event.PartnerVerificationDecidedEvent;
import com.portal.platformservice.exception.ResourceNotFoundException;
import com.portal.platformservice.mapper.PartnerVerificationMapper;
import com.portal.platformservice.repository.PartnerVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerVerificationService {

    private final PartnerVerificationRepository verificationRepository;
    private final ApprovalHistoryService approvalHistoryService;
    private final PartnerVerificationStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PartnerVerificationResponse submit(PartnerVerificationSubmitRequest request) {
        PartnerVerification verification = PartnerVerification.builder()
                .userId(request.getUserId())
                .organizationNameSnapshot(request.getOrganizationNameSnapshot())
                .contactEmailSnapshot(request.getContactEmailSnapshot())
                .build();

        verification = verificationRepository.save(verification);

        approvalHistoryService.record(ApprovalType.PARTNER_VERIFICATION, verification.getId(),
                null, verification.getStatus().name(), request.getUserId(), "Verification submitted");

        return PartnerVerificationMapper.toResponse(verification);
    }

    @Transactional(readOnly = true)
    public PartnerVerificationResponse getById(UUID id) {
        return PartnerVerificationMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PartnerVerificationResponse getByUserId(UUID userId) {
        PartnerVerification verification = verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner verification not found for user: " + userId));
        return PartnerVerificationMapper.toResponse(verification);
    }

    @Transactional(readOnly = true)
    public Page<PartnerVerificationSummaryResponse> listByStatus(VerificationStatus status, Pageable pageable) {
        return verificationRepository.findByStatus(status, pageable)
                .map(PartnerVerificationMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<PartnerVerificationSummaryResponse> listAll(Pageable pageable) {
        return verificationRepository.findAll(pageable)
                .map(PartnerVerificationMapper::toSummaryResponse);
    }

    @Transactional
    public PartnerVerificationResponse reapply(UUID id) {
        PartnerVerification verification = findOrThrow(id);
        transition(verification, VerificationStatus.PENDING_DOCUMENTS, verification.getUserId(), "Company re-applied after rejection");
        verification.setRejectionReason(null);
        verificationRepository.flush();
        return PartnerVerificationMapper.toResponse(verification);
    }

    @Transactional
    public PartnerVerificationResponse submitForReview(UUID id) {
        PartnerVerification verification = findOrThrow(id);
        // Whether all required document types are present is not checked
        // here: that list is meant to come from SystemConfigService
        // (config key partner.required.document.types), which doesn't
        // exist yet. Add that check here once it does.
        transition(verification, VerificationStatus.PENDING_REVIEW, null, "Submitted for review");
        verificationRepository.flush();
        return PartnerVerificationMapper.toResponse(verification);
    }

    @Transactional
    public PartnerVerificationResponse claim(UUID id, UUID reviewerId) {
        PartnerVerification verification = findOrThrow(id);
        transition(verification, VerificationStatus.UNDER_REVIEW, reviewerId, "Claimed by reviewer");
        verification.setReviewedByUserId(reviewerId);
        verificationRepository.flush();
        return PartnerVerificationMapper.toResponse(verification);
    }

    @Transactional
    public PartnerVerificationResponse decide(UUID id, PartnerVerificationDecisionRequest request) {
        PartnerVerification verification = findOrThrow(id);

        VerificationStatus target = switch (request.getDecision()) {
            case APPROVE -> VerificationStatus.APPROVED;
            case REJECT -> VerificationStatus.REJECTED;
            case REQUEST_MORE_INFO -> VerificationStatus.MORE_INFO_REQUIRED;
        };

        transition(verification, target, request.getActingUserId(), request.getDecisionNotes());

        verification.setReviewedByUserId(request.getActingUserId());
        verification.setReviewedAt(Instant.now());
        verification.setDecisionNotes(request.getDecisionNotes());

        if (target == VerificationStatus.REJECTED) {
            verification.setRejectionReason(request.getRejectionReason());
        }

        if (target == VerificationStatus.APPROVED || target == VerificationStatus.REJECTED) {
            verification.setSyncStatus(SyncStatus.PENDING_CALLBACK);
            eventPublisher.publishEvent(new PartnerVerificationDecidedEvent(
                    verification.getId(), verification.getUserId(), target));
        }

        verificationRepository.flush();
        return PartnerVerificationMapper.toResponse(verification);
    }

    @Transactional
    public PartnerVerificationResponse adminEdit(UUID id, PartnerVerificationAdminEditRequest request) {
        PartnerVerification verification = findOrThrow(id);

        StringBuilder remarks = new StringBuilder("Administrative edit:");
        boolean changed = false;

        if (request.getOrganizationNameSnapshot() != null) {
            verification.setOrganizationNameSnapshot(request.getOrganizationNameSnapshot());
            remarks.append(" organizationNameSnapshot;");
            changed = true;
        }
        if (request.getContactEmailSnapshot() != null) {
            verification.setContactEmailSnapshot(request.getContactEmailSnapshot());
            remarks.append(" contactEmailSnapshot;");
            changed = true;
        }
        if (request.getDecisionNotes() != null) {
            verification.setDecisionNotes(request.getDecisionNotes());
            remarks.append(" decisionNotes;");
            changed = true;
        }
        if (request.getRejectionReason() != null) {
            verification.setRejectionReason(request.getRejectionReason());
            remarks.append(" rejectionReason;");
            changed = true;
        }
        if (request.getReviewedByUserId() != null) {
            verification.setReviewedByUserId(request.getReviewedByUserId());
            remarks.append(" reviewedByUserId;");
            changed = true;
        }

        if (changed) {
            approvalHistoryService.record(ApprovalType.PARTNER_VERIFICATION, verification.getId(),
                    verification.getStatus().name(), verification.getStatus().name(),
                    request.getActingUserId(), remarks.toString());
            verificationRepository.flush();
        }

        return PartnerVerificationMapper.toResponse(verification);
    }

    private void transition(PartnerVerification verification, VerificationStatus target,
                             UUID changedByUserId, String remarks) {
        VerificationStatus current = verification.getStatus();
        stateMachine.validateTransition(current, target);
        verification.setStatus(target);

        approvalHistoryService.record(ApprovalType.PARTNER_VERIFICATION, verification.getId(),
                current.name(), target.name(), changedByUserId, remarks);
    }

    private PartnerVerification findOrThrow(UUID id) {
        return verificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner verification not found: " + id));
    }
}
