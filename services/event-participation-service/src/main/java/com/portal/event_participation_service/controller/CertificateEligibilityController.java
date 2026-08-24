package com.portal.event_participation_service.controller;

import com.portal.event_participation_service.dto.CertificateEligibilityResponse;
import com.portal.event_participation_service.service.CertificateEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificate-eligibility")
@RequiredArgsConstructor
public class CertificateEligibilityController {

    private final CertificateEligibilityService certificateEligibilityService;

    @GetMapping("/registration/{registrationId}")
    public ResponseEntity<CertificateEligibilityResponse> getByRegistration(@PathVariable UUID registrationId) {
        return ResponseEntity.ok(certificateEligibilityService.getByRegistration(registrationId));
    }
}