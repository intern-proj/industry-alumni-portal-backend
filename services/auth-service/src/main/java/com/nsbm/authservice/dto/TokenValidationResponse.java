package com.nsbm.authservice.dto;

public record TokenValidationResponse(
        boolean valid,
        String username,
        String email,
        String role,
        String userType
) {}

