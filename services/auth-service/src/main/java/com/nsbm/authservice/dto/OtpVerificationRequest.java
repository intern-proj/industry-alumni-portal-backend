package com.nsbm.authservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record OtpVerificationRequest(
        @JsonAlias({"tempToken", "sessionToken"})
        @NotBlank(message = "Session token or username is required")
        String sessionToken,

        @NotBlank(message = "OTP code is required")
        String otpCode
) {}