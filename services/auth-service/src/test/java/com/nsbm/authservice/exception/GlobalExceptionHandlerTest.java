package com.nsbm.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/auth/test");
    }

    @Test
    @DisplayName("handleStaffAlreadyExists should return 409 CONFLICT with ProblemDetail")
    void handleStaffAlreadyExists_ReturnsConflictResponse() {
        // Arrange
        StaffAlreadyExistsException exception = new StaffAlreadyExistsException("Staff with email already exists");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleStaffAlreadyExists(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getTitle()).isEqualTo("Resource Conflict");
        assertThat(body.getDetail()).isEqualTo("Staff with email already exists");
        assertThat(body.getType().toString()).isEqualTo("https://portal.domain.com/errors/conflict");
        assertThat(body.getInstance().toString()).isEqualTo("/api/v1/auth/test");
        assertThat(body.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleInvalidToken should return 400 BAD_REQUEST with ProblemDetail")
    void handleInvalidToken_ReturnsBadRequestResponse() {
        // Arrange
        InvalidTokenException exception = new InvalidTokenException("Invalid token provided");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleInvalidToken(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getTitle()).isEqualTo("Invalid Token");
        assertThat(body.getDetail()).isEqualTo("Invalid token provided");
        assertThat(body.getType().toString()).isEqualTo("https://portal.domain.com/errors/invalid-token");
        assertThat(body.getInstance().toString()).isEqualTo("/api/v1/auth/test");
        assertThat(body.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleUsernameExists should return 409 CONFLICT with ProblemDetail")
    void handleUsernameExists_ReturnsConflictResponse() {
        // Arrange
        UsernameAlreadyExistsException exception = new UsernameAlreadyExistsException("Username taken");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleUsernameExists(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getTitle()).isEqualTo("Username Already Exists");
        assertThat(body.getDetail()).isEqualTo("Username taken");
        assertThat(body.getType().toString()).isEqualTo("https://portal.domain.com/errors/username-conflict");
        assertThat(body.getInstance().toString()).isEqualTo("/api/v1/auth/test");
        assertThat(body.getProperties()).containsKey("timestamp");
    }
}
