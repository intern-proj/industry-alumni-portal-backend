package com.nsbm.eventmanagementservice.controller;

import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaRequest;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaResponse;
import com.nsbm.eventmanagementservice.service.CertificateEligibilityCriteriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/events/{eventId}/eligibility-criteria", "/api/v1/events/{eventId}/eligibility-criteria"})
@RequiredArgsConstructor
public class CertificateEligibilityCriteriaController {
    private final CertificateEligibilityCriteriaService criteriaService;

    @PutMapping
    public ResponseEntity<CertificateEligibilityCriteriaResponse> createOrUpdateCriteria(
            @PathVariable Long eventId,
            @Valid @RequestBody CertificateEligibilityCriteriaRequest request) {
        return ResponseEntity.ok(criteriaService.createOrUpdateCriteria(eventId, request));
    }

    @GetMapping
    public ResponseEntity<CertificateEligibilityCriteriaResponse> getCriteria(@PathVariable Long eventId) {
        return ResponseEntity.ok(criteriaService.getCriteriaByEventId(eventId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCriteria(@PathVariable Long eventId) {
        criteriaService.deleteCriteria(eventId);
        return ResponseEntity.noContent().build();
    }
}
