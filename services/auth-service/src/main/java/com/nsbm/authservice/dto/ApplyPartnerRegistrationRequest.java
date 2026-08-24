package com.nsbm.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApplyPartnerRegistrationRequest(
        @NotBlank(message = "Representative's full name is required")
        String representativeFullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "Representative's job role is required")
        String representativeJobRole,

        @NotBlank(message = "Company name is required")
        String companyName,

        @NotBlank(message = "Company industry is required")
        String companyIndustry,

        @NotBlank(message = "Company address is required")
        String companyAddress,

        @NotBlank(message = "Company description is required")
        String companyDescription
) {}
