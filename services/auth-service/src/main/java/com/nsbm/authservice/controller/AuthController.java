package com.nsbm.authservice.controller;

import com.nsbm.authservice.dto.*;
import com.nsbm.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/staff/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inviteStaff(@Valid @RequestBody StaffInvitationRequest request) {
        authService.inviteStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/staff/complete-registration")
    public ResponseEntity<Void> completeStaffRegistration(@Valid @RequestBody CompleteStaffRegistrationRequest request) {
        authService.completeStaffRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/partner/pending")
    public ResponseEntity<Void> createPendingPartner(@Valid @RequestBody ApplyPartnerRegistrationRequest request) {
        authService.createPendingPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


}
