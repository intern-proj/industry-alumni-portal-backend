package com.portal.platformservice.controller;

import com.portal.platformservice.dto.request.VacancyApprovalAdminEditRequest;
import com.portal.platformservice.dto.request.VacancyApprovalDecisionRequest;
import com.portal.platformservice.dto.response.ApprovalHistoryResponse;
import com.portal.platformservice.dto.response.VacancyApprovalResponse;
import com.portal.platformservice.dto.response.VacancyApprovalSummaryResponse;
import com.portal.platformservice.entity.ApprovalType;
import com.portal.platformservice.entity.VacancyApprovalStatus;
import com.portal.platformservice.service.ApprovalHistoryService;
import com.portal.platformservice.service.VacancyApprovalService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vacancy-approvals")
@RequiredArgsConstructor
public class VacancyApprovalAdminController {

    private final VacancyApprovalService vacancyApprovalService;
    private final ApprovalHistoryService approvalHistoryService;

    @GetMapping
    public Page<VacancyApprovalSummaryResponse> list(
            @RequestParam VacancyApprovalStatus status, Pageable pageable) {
        return vacancyApprovalService.listByStatus(status, pageable);
    }

    @GetMapping("/{id}")
    public VacancyApprovalResponse getById(@PathVariable UUID id) {
        return vacancyApprovalService.getById(id);
    }

    @GetMapping("/{id}/history")
    public List<ApprovalHistoryResponse> getHistory(@PathVariable UUID id) {
        return approvalHistoryService.getHistory(ApprovalType.VACANCY_APPROVAL, id);
    }

    @PostMapping("/{id}/claim")
    public VacancyApprovalResponse claim(@PathVariable UUID id, @RequestParam UUID reviewerId) {
        return vacancyApprovalService.claim(id, reviewerId);
    }

    @PostMapping("/{id}/decision")
    public VacancyApprovalResponse decide(
            @PathVariable UUID id, @Valid @RequestBody VacancyApprovalDecisionRequest request) {
        return vacancyApprovalService.decide(id, request);
    }

    @PatchMapping("/{id}")
    public VacancyApprovalResponse adminEdit(
            @PathVariable UUID id, @Valid @RequestBody VacancyApprovalAdminEditRequest request) {
        return vacancyApprovalService.adminEdit(id, request);
    }
}
