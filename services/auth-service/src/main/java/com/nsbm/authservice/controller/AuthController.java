package com.nsbm.authservice.controller;

import com.nsbm.authservice.dto.ApplyPartnerRegistrationRequest;
import com.nsbm.authservice.dto.AuthResponse;
import com.nsbm.authservice.dto.CompletePartnerRegistrationRequest;
import com.nsbm.authservice.dto.CompleteStaffRegistrationRequest;
import com.nsbm.authservice.dto.CreateAdminRequest;
import com.nsbm.authservice.dto.ForgotPasswordRequest;
import com.nsbm.authservice.dto.LoginRequest;
import com.nsbm.authservice.dto.LoginResponse;
import com.nsbm.authservice.dto.OtpVerificationRequest;
import com.nsbm.authservice.dto.ResetPasswordRequest;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.dto.Step1LoginResponse;
import com.nsbm.authservice.dto.TokenValidationResponse;
import com.nsbm.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PostMapping("/admin/create")
    public ResponseEntity<Void> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        authService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @DeleteMapping("/user/{identifier}")
    public ResponseEntity<Void> deleteUser(@PathVariable String identifier) {
        authService.deleteUser(identifier);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping("/staff/invite")
    public ResponseEntity<Void> inviteStaff(@Valid @RequestBody StaffInvitationRequest request) {
        authService.inviteStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'FACULTY_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @DeleteMapping("/staff/invite")
    public ResponseEntity<Void> revokeStaffInvitation(@RequestParam String email) {
        authService.revokeStaffInvitation(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/staff/complete-registration")
    public ResponseEntity<Map<String, String>> completeStaffRegistration(@Valid @RequestBody CompleteStaffRegistrationRequest request) {
        String email = authService.completeStaffRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("email", email));
    }

    @PostMapping("/partner/pending")
    public ResponseEntity<Void> createPendingPartner(@Valid @RequestBody ApplyPartnerRegistrationRequest request) {
        authService.createPendingPartner(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/partner/complete-registration")
    public ResponseEntity<Map<String, String>> completePartnerRegistration(@Valid @RequestBody CompletePartnerRegistrationRequest request) {
        String email = authService.completePartnerRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("email", email));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/admin/login", "/staff/login"})
    public ResponseEntity<Step1LoginResponse> initiateAdminLogin(@Valid @RequestBody LoginRequest request) {
        Step1LoginResponse response = authService.initiateAdminLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/verify-otp", "/admin/verify-otp", "/staff/verify-otp"})
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        AuthResponse response = authService.verifyOtp(request);
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
