package com.nsbm.authservice.dto;

public record Step1LoginResponse(
        String sessionToken,
        String username,
        String message,
        long expiresInSeconds
) {}