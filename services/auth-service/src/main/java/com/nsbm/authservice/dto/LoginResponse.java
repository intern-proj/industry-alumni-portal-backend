package com.nsbm.authservice.dto;

public record LoginResponse(
        boolean requiresOtp,
        String sessionToken,
        String accessToken,
        String tokenType,
        String username,
        String email,
        String role,
        String userType,
        String message,
        Long expiresInSeconds
) {
    public static LoginResponse direct(AuthResponse auth) {
        return new LoginResponse(
                false,
                null,
                auth.accessToken(),
                auth.tokenType(),
                auth.username(),
                auth.email(),
                auth.role(),
                auth.userType(),
                "Login successful.",
                null
        );
    }

    public static LoginResponse otpRequired(String sessionToken, String username, String message, long expiresInSeconds) {
        return new LoginResponse(
                true,
                sessionToken,
                null,
                null,
                username,
                null,
                null,
                null,
                message,
                expiresInSeconds
        );
    }
}
