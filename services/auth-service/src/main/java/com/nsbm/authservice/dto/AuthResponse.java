package com.nsbm.authservice.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String username,
        String email,
        String role,
        String userType
) {
    public AuthResponse(String accessToken, String username, String email, String role, String userType) {
        this(accessToken, "Bearer", username, email, role, userType);
    }
}

