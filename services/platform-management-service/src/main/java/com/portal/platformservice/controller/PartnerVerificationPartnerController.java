package com.portal.platformservice.controller;

import com.portal.platformservice.dto.request.PartnerVerificationSubmitRequest;
import com.portal.platformservice.dto.response.PartnerVerificationResponse;
import com.portal.platformservice.exception.ResourceNotFoundException;
import com.portal.platformservice.service.PartnerVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partner-verifications")
@RequiredArgsConstructor
public class PartnerVerificationPartnerController {

    private final PartnerVerificationService verificationService;

    @PreAuthorize("hasRole('INDUSTRY_PARTNER')")
    @GetMapping("/me")
    public ResponseEntity<PartnerVerificationResponse> getMyVerificationStatus(Principal principal) {
        UUID generatedUserId = UUID.nameUUIDFromBytes(principal.getName().getBytes());
        try {
            return ResponseEntity.ok(verificationService.getByUserId(generatedUserId));
        } catch (ResourceNotFoundException e) {
            // Lazy initialization for partner verification
            PartnerVerificationSubmitRequest req = new PartnerVerificationSubmitRequest();
            req.setUserId(generatedUserId);
            req.setOrganizationNameSnapshot(principal.getName());
            req.setContactEmailSnapshot(principal.getName() + "@example.com"); // Placeholder
            return ResponseEntity.ok(verificationService.submit(req));
        }
    }
    @PreAuthorize("hasRole('INDUSTRY_PARTNER')")
    @PostMapping("/me/reapply")
    public ResponseEntity<PartnerVerificationResponse> reapply(Principal principal) {
        UUID generatedUserId = UUID.nameUUIDFromBytes(principal.getName().getBytes());
        PartnerVerificationResponse verification = verificationService.getByUserId(generatedUserId);
        return ResponseEntity.ok(verificationService.reapply(verification.getId()));
    }
}
