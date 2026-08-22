package com.nsbm.authservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMilliseconds", TEST_EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should successfully generate JWT token and extract claims")
    void generateTokenAndExtractClaims_Success() {
        // Arrange
        String username = "john_student";
        String email = "john@student.nsbm.ac.lk";
        String role = "STUDENT";
        String userType = "STUDENT";

        // Act
        String token = jwtTokenProvider.generateToken(username, email, role, userType);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(role);
        assertThat(jwtTokenProvider.getUserTypeFromToken(token)).isEqualTo(userType);
    }

    @Test
    @DisplayName("Should return false when validating an invalid or malformed token")
    void validateToken_ReturnsFalse_ForInvalidToken() {
        // Arrange
        String malformedToken = "invalid.jwt.token.string";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false when validating an expired token")
    void validateToken_ReturnsFalse_ForExpiredToken() {
        // Arrange - set negative expiration time to simulate expired token
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMilliseconds", -1000L);
        String expiredToken = jwtTokenProvider.generateToken("user", "user@test.com", "ADMIN", "MANAGEMENT_STAFF");

        // Act
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Assert
        assertThat(isValid).isFalse();
    }
}
