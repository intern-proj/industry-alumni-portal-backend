package com.portal.platformservice.controller;

import com.portal.platformservice.dto.request.PartnerVerificationAdminEditRequest;
import com.portal.platformservice.dto.request.PartnerVerificationDecisionRequest;
import com.portal.platformservice.dto.response.ApprovalHistoryResponse;
import com.portal.platformservice.dto.response.PartnerVerificationResponse;
import com.portal.platformservice.dto.response.PartnerVerificationSummaryResponse;
import com.portal.platformservice.entity.ApprovalType;
import com.portal.platformservice.entity.VerificationStatus;
import com.portal.platformservice.service.ApprovalHistoryService;
import com.portal.platformservice.service.PartnerVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/partner-verifications")
@RequiredArgsConstructor
public class PartnerVerificationAdminController {

    private final PartnerVerificationService partnerVerificationService;
    private final ApprovalHistoryService approvalHistoryService;

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT')")
    @GetMapping
    public Page<PartnerVerificationSummaryResponse> list(
            @RequestParam(required = false) VerificationStatus status, Pageable pageable) {
        if (status == null) {
            return partnerVerificationService.listAll(pageable);
        }
        return partnerVerificationService.listByStatus(status, pageable);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT')")
    @GetMapping("/{id}")
    public PartnerVerificationResponse getById(@PathVariable UUID id) {
        return partnerVerificationService.getById(id);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT')")
    @GetMapping("/{id}/history")
    public List<ApprovalHistoryResponse> getHistory(@PathVariable UUID id) {
        return approvalHistoryService.getHistory(ApprovalType.PARTNER_VERIFICATION, id);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping("/{id}/claim")
    public PartnerVerificationResponse claim(@PathVariable UUID id, @RequestParam UUID reviewerId) {
        return partnerVerificationService.claim(id, reviewerId);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping("/{id}/decision")
    public PartnerVerificationResponse decide(
            @PathVariable UUID id, @Valid @RequestBody PartnerVerificationDecisionRequest request) {
        return partnerVerificationService.decide(id, request);
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PatchMapping("/{id}")
    public PartnerVerificationResponse adminEdit(
            @PathVariable UUID id, @Valid @RequestBody PartnerVerificationAdminEditRequest request) {
        return partnerVerificationService.adminEdit(id, request);
    }
}
