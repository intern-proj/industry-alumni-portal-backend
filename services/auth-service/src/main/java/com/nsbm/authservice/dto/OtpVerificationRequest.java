package com.nsbm.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerificationRequest(
        @NotBlank(message = "Session token or username is required")
        String sessionToken,

        @NotBlank(message = "OTP code is required")
        String otpCode
) {}