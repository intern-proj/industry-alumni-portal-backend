package com.portal.platformservice.service;

import com.portal.platformservice.dto.request.VacancyApprovalAdminEditRequest;
import com.portal.platformservice.dto.request.VacancyApprovalDecisionRequest;
import com.portal.platformservice.dto.request.VacancyApprovalSubmitRequest;
import com.portal.platformservice.dto.response.VacancyApprovalResponse;
import com.portal.platformservice.dto.response.VacancyApprovalSummaryResponse;
import com.portal.platformservice.entity.ApprovalType;
import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VacancyApproval;
import com.portal.platformservice.entity.VacancyApprovalStatus;
import com.portal.platformservice.event.VacancyApprovalDecidedEvent;
import com.portal.platformservice.exception.ResourceNotFoundException;
import com.portal.platformservice.mapper.VacancyApprovalMapper;
import com.portal.platformservice.repository.VacancyApprovalRepository;
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
public class VacancyApprovalService {

    private final VacancyApprovalRepository approvalRepository;
    private final ApprovalHistoryService approvalHistoryService;
    private final VacancyApprovalStateMachine stateMachine;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public VacancyApprovalResponse submit(VacancyApprovalSubmitRequest request) {
        VacancyApproval approval = VacancyApproval.builder()
                .vacancyId(request.getVacancyId())
                .companyUserId(request.getCompanyUserId())
                .submittedByUserId(request.getSubmittedByUserId())
                .vacancyTitleSnapshot(request.getVacancyTitleSnapshot())
                .companyNameSnapshot(request.getCompanyNameSnapshot())
                .build();

        approval = approvalRepository.save(approval);

        approvalHistoryService.record(ApprovalType.VACANCY_APPROVAL, approval.getId(),
                null, approval.getStatus().name(), request.getSubmittedByUserId(), "Vacancy submitted for approval");

        return VacancyApprovalMapper.toResponse(approval);
    }

    @Transactional(readOnly = true)
    public VacancyApprovalResponse getById(UUID id) {
        return VacancyApprovalMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<VacancyApprovalSummaryResponse> listByStatus(VacancyApprovalStatus status, Pageable pageable) {
        return approvalRepository.findByStatus(status, pageable)
                .map(VacancyApprovalMapper::toSummaryResponse);
    }

    @Transactional
    public VacancyApprovalResponse claim(UUID id, UUID reviewerId) {
        VacancyApproval approval = findOrThrow(id);
        transition(approval, VacancyApprovalStatus.UNDER_REVIEW, reviewerId, "Claimed by reviewer");
        approval.setAssignedReviewerId(reviewerId);
        approvalRepository.flush();
        return VacancyApprovalMapper.toResponse(approval);
    }

    @Transactional
    public VacancyApprovalResponse decide(UUID id, VacancyApprovalDecisionRequest request) {
        VacancyApproval approval = findOrThrow(id);

        VacancyApprovalStatus target = switch (request.getDecision()) {
            case APPROVE -> VacancyApprovalStatus.APPROVED;
            case REJECT -> VacancyApprovalStatus.REJECTED;
        };

        transition(approval, target, request.getActingUserId(), request.getDecisionNotes());

        approval.setReviewedByUserId(request.getActingUserId());
        approval.setReviewedAt(Instant.now());
        approval.setDecisionNotes(request.getDecisionNotes());

        if (target == VacancyApprovalStatus.REJECTED) {
            approval.setRejectionReason(request.getRejectionReason());
        }

        approval.setSyncStatus(SyncStatus.PENDING_CALLBACK);
        eventPublisher.publishEvent(new VacancyApprovalDecidedEvent(
                approval.getId(), approval.getVacancyId(), target));

        approvalRepository.flush();
        return VacancyApprovalMapper.toResponse(approval);
    }

    @Transactional
    public VacancyApprovalResponse adminEdit(UUID id, VacancyApprovalAdminEditRequest request) {
        VacancyApproval approval = findOrThrow(id);

        StringBuilder remarks = new StringBuilder("Administrative edit:");
        boolean changed = false;

        if (request.getVacancyTitleSnapshot() != null) {
            approval.setVacancyTitleSnapshot(request.getVacancyTitleSnapshot());
            remarks.append(" vacancyTitleSnapshot;");
            changed = true;
        }
        if (request.getCompanyNameSnapshot() != null) {
            approval.setCompanyNameSnapshot(request.getCompanyNameSnapshot());
            remarks.append(" companyNameSnapshot;");
            changed = true;
        }
        if (request.getDecisionNotes() != null) {
            approval.setDecisionNotes(request.getDecisionNotes());
            remarks.append(" decisionNotes;");
            changed = true;
        }
        if (request.getRejectionReason() != null) {
            approval.setRejectionReason(request.getRejectionReason());
            remarks.append(" rejectionReason;");
            changed = true;
        }
        if (request.getAssignedReviewerId() != null) {
            approval.setAssignedReviewerId(request.getAssignedReviewerId());
            remarks.append(" assignedReviewerId;");
            changed = true;
        }

        if (changed) {
            approvalHistoryService.record(ApprovalType.VACANCY_APPROVAL, approval.getId(),
                    approval.getStatus().name(), approval.getStatus().name(),
                    request.getActingUserId(), remarks.toString());
            approvalRepository.flush();
        }

        return VacancyApprovalMapper.toResponse(approval);
    }

    private void transition(VacancyApproval approval, VacancyApprovalStatus target,
                             UUID changedByUserId, String remarks) {
        VacancyApprovalStatus current = approval.getStatus();
        stateMachine.validateTransition(current, target);
        approval.setStatus(target);

        approvalHistoryService.record(ApprovalType.VACANCY_APPROVAL, approval.getId(),
                current.name(), target.name(), changedByUserId, remarks);
    }

    private VacancyApproval findOrThrow(UUID id) {
        return approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy approval not found: " + id));
    }
}
