package com.portal.platformservice.controller;

import com.portal.platformservice.dto.request.VacancyApprovalSubmitRequest;
import com.portal.platformservice.dto.response.VacancyApprovalResponse;
import com.portal.platformservice.service.VacancyApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Vacancy Service (via Feign, once that client exists) whenever
 * a vacancy is submitted and needs a governance/review record opened here.
 */
@RestController
@RequestMapping("/api/v1/internal/vacancy-approvals")
@RequiredArgsConstructor
public class VacancyApprovalInternalController {

    private final VacancyApprovalService vacancyApprovalService;

    @PostMapping
    public ResponseEntity<VacancyApprovalResponse> submit(
            @Valid @RequestBody VacancyApprovalSubmitRequest request) {
        VacancyApprovalResponse response = vacancyApprovalService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
