package com.portal.platformservice.controller;

import com.nsbm.common.security.JwtTokenProvider;
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
    private final JwtTokenProvider tokenProvider;

    @PreAuthorize("hasRole('INDUSTRY_PARTNER')")
    @GetMapping("/me")
    public ResponseEntity<PartnerVerificationResponse> getMyVerificationStatus(
            Principal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        UUID generatedUserId = UUID.nameUUIDFromBytes(principal.getName().getBytes());
        String email = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                email = tokenProvider.getEmailFromToken(authHeader.substring(7));
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(verificationService.getOrLinkVerification(generatedUserId, email, principal.getName()));
    }
    @PreAuthorize("hasRole('INDUSTRY_PARTNER')")
    @PostMapping("/me/reapply")
    public ResponseEntity<PartnerVerificationResponse> reapply(Principal principal) {
        UUID generatedUserId = UUID.nameUUIDFromBytes(principal.getName().getBytes());
        PartnerVerificationResponse verification = verificationService.getByUserId(generatedUserId);
        return ResponseEntity.ok(verificationService.reapply(verification.getId()));
    }
}
