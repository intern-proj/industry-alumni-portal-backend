package com.nsbm.authservice.controller;

import com.nsbm.authservice.entity.IndustryPartner;
import com.nsbm.authservice.exception.ResourceNotFoundException;
import com.nsbm.authservice.repository.IndustryPartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

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

    @PreAuthorize("hasRole('INDUSTRY_PARTNER')")
    @org.springframework.web.bind.annotation.PutMapping("/me")
    public ResponseEntity<IndustryPartner> updateMyProfile(Principal principal, @org.springframework.web.bind.annotation.RequestBody IndustryPartner updateData) {
        IndustryPartner partner = industryPartnerRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found"));

        if (updateData.getCompanyName() != null && !updateData.getCompanyName().isBlank()) {
            partner.setCompanyName(updateData.getCompanyName());
        }
        if (updateData.getCompanyIndustry() != null) {
            partner.setCompanyIndustry(updateData.getCompanyIndustry());
        }
        if (updateData.getCompanyDescription() != null) {
            partner.setCompanyDescription(updateData.getCompanyDescription());
        }
        if (updateData.getCompanyAddress() != null) {
            partner.setCompanyAddress(updateData.getCompanyAddress());
        }
        if (updateData.getRepresentativeFullName() != null) {
            partner.setRepresentativeFullName(updateData.getRepresentativeFullName());
        }
        if (updateData.getRepresentativeJobRole() != null) {
            partner.setRepresentativeJobRole(updateData.getRepresentativeJobRole());
        }
        if (updateData.getPhone() != null) {
            partner.setPhone(updateData.getPhone());
        }
        if (updateData.getLogoUrl() != null) {
            partner.setLogoUrl(updateData.getLogoUrl());
        }
        if (updateData.getWebsite() != null) {
            partner.setWebsite(updateData.getWebsite());
        }
        if (updateData.getCompanySize() != null) {
            partner.setCompanySize(updateData.getCompanySize());
        }

        IndustryPartner saved = industryPartnerRepository.save(partner);
        saved.setPasswordHash(null);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/directory")
    public ResponseEntity<List<IndustryPartner>> getPublicPartnerDirectory() {
        List<IndustryPartner> activePartners = industryPartnerRepository.findAll().stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getAccountStatus()))
                .peek(p -> p.setPasswordHash(null))
                .toList();
        return ResponseEntity.ok(activePartners);
    }

    @GetMapping("/directory/{id}")
    public ResponseEntity<IndustryPartner> getPublicPartnerById(@PathVariable Long id) {
        IndustryPartner partner = industryPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company partner not found with ID: " + id));
        partner.setPasswordHash(null);
        return ResponseEntity.ok(partner);
    }
}
