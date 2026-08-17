package com.portal.platformservice.controller;

import com.portal.platformservice.dto.request.PartnerVerificationSubmitRequest;
import com.portal.platformservice.dto.response.PartnerVerificationResponse;
import com.portal.platformservice.service.PartnerVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by User Service (via Feign, once that client exists) whenever a
 * new partner account is registered, to open a governance record here.
 */
@RestController
@RequestMapping("/api/v1/internal/partner-verifications")
@RequiredArgsConstructor
public class PartnerVerificationInternalController {

    private final PartnerVerificationService partnerVerificationService;

    @PostMapping
    public ResponseEntity<PartnerVerificationResponse> submit(
            @Valid @RequestBody PartnerVerificationSubmitRequest request) {
        PartnerVerificationResponse response = partnerVerificationService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
