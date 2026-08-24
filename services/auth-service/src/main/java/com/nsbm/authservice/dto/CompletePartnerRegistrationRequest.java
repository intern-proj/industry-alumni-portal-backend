package com.nsbm.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CompletePartnerRegistrationRequest(
        @NotBlank(message = "Registration token is required")
        String registrationToken,

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
