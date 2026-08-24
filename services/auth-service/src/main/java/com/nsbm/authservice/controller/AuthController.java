package com.nsbm.authservice.controller;

import com.nsbm.authservice.dto.*;
import com.nsbm.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/partner/complete-registration")
    public ResponseEntity<Void> completePartnerRegistration(@Valid @RequestBody CompletePartnerRegistrationRequest request) {
        authService.completePartnerRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginStudentOrPartner(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginStudentOrPartner(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff/login")
    public ResponseEntity<Step1LoginResponse> initiateStaffLogin(@Valid @RequestBody LoginRequest request) {
        Step1LoginResponse response = authService.initiateStaffLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff/verify-otp")
    public ResponseEntity<AuthResponse> verifyStaffOtp(@Valid @RequestBody OtpVerificationRequest request) {
        AuthResponse response = authService.verifyStaffOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam(value = "token", required = false) String tokenParam,
                                                                 @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = tokenParam;
        if ((token == null || token.isEmpty()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        TokenValidationResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<TokenValidationResponse> getCurrentUser(Authentication authentication,
                                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        TokenValidationResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

}
