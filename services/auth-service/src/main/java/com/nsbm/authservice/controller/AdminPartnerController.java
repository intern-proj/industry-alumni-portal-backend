package com.nsbm.authservice.controller;

import com.nsbm.authservice.entity.IndustryPartner;
import com.nsbm.authservice.entity.PendingPartner;
import com.nsbm.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPartnerController {

    private final AuthService authService;

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'INTERNSHIP_COORDINATOR')")
    @GetMapping("/pending-partners")
    public ResponseEntity<List<PendingPartner>> getAllPendingPartners() {
        return ResponseEntity.ok(authService.getAllPendingPartners());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'INTERNSHIP_COORDINATOR')")
    @PostMapping("/pending-partners/{id}/approve")
    public ResponseEntity<Void> approvePendingPartner(@PathVariable Long id) {
        authService.approvePendingPartner(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'INTERNSHIP_COORDINATOR')")
    @PostMapping("/pending-partners/{id}/reject")
    public ResponseEntity<Void> rejectPendingPartner(@PathVariable Long id) {
        authService.rejectPendingPartner(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'INTERNSHIP_COORDINATOR')")
    @GetMapping("/partners")
    public ResponseEntity<List<IndustryPartner>> getAllIndustryPartners() {
        return ResponseEntity.ok(authService.getAllIndustryPartners());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'INTERNSHIP_COORDINATOR')")
    @PutMapping("/partners/{id}/status")
    public ResponseEntity<Void> toggleIndustryPartnerStatus(@PathVariable Long id) {
        authService.toggleIndustryPartnerStatus(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'INTERNSHIP_COORDINATOR')")
    @DeleteMapping("/partners/{id}")
    public ResponseEntity<Void> deleteIndustryPartner(@PathVariable Long id) {
        authService.deleteIndustryPartner(id);
        return ResponseEntity.ok().build();
    }
}
