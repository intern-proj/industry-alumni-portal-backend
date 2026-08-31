package com.nsbm.authservice.controller;

import com.nsbm.authservice.entity.IndustryPartner;
import com.nsbm.authservice.exception.ResourceNotFoundException;
import com.nsbm.authservice.repository.IndustryPartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth/partner")
@RequiredArgsConstructor
public class PartnerProfileController {

    private final IndustryPartnerRepository industryPartnerRepository;

    @PreAuthorize("hasRole('INDUSTRY_PARTNER')")
    @GetMapping("/me")
    public ResponseEntity<IndustryPartner> getMyProfile(Principal principal) {
        IndustryPartner partner = industryPartnerRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));
        // Remove sensitive info
        partner.setPasswordHash(null);
        return ResponseEntity.ok(partner);
    }
}
