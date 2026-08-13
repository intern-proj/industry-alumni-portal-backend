package com.nsbm.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.authservice.dto.CompleteStaffRegistrationRequest;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.exception.GlobalExceptionHandler;
import com.nsbm.authservice.exception.InvalidTokenException;
import com.nsbm.authservice.exception.StaffAlreadyExistsException;
import com.nsbm.authservice.exception.UsernameAlreadyExistsException;
import com.nsbm.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/staff/invite Tests")
    class InviteStaffApiTests {

        @Test
        @DisplayName("Should return 201 Created for valid staff invitation request")
        void inviteStaff_Returns201Created() throws Exception {
            // Arrange
            StaffInvitationRequest request = new StaffInvitationRequest("lecturer@nsbm.ac.lk", Role.ACADEMIC_STAFF);
            doNothing().when(authService).inviteStaff(any(StaffInvitationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(authService, times(1)).inviteStaff(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email format is invalid")
        void inviteStaff_Returns400BadRequest_WhenInvalidEmail() throws Exception {
            // Arrange - Invalid email string
            String invalidJson = """
                    {
                        "email": "not-an-email",
                        "role": "LECTURER"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).inviteStaff(any());
        }

        @Test
        @DisplayName("Should return 409 Conflict when staff already exists")
        void inviteStaff_Returns409Conflict_WhenStaffAlreadyExists() throws Exception {
            // Arrange
            StaffInvitationRequest request = new StaffInvitationRequest("existing@nsbm.ac.lk", Role.ADMIN);
            doThrow(new StaffAlreadyExistsException("Staff member with email existing@nsbm.ac.lk is already registered or invited."))
                    .when(authService).inviteStaff(any(StaffInvitationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/invite")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource Conflict"))
                    .andExpect(jsonPath("$.detail").value("Staff member with email existing@nsbm.ac.lk is already registered or invited."));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/staff/complete-registration Tests")
    class CompleteStaffRegistrationApiTests {

        @Test
        @DisplayName("Should return 201 Created for valid complete registration request")
        void completeStaffRegistration_Returns201Created() throws Exception {
            // Arrange
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    "token-uuid-1234", "john_doe", "SecurePassword123"
            );
            doNothing().when(authService).completeStaffRegistration(any(CompleteStaffRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(authService, times(1)).completeStaffRegistration(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when validation fails (e.g. short password)")
        void completeStaffRegistration_Returns400BadRequest_WhenShortPassword() throws Exception {
            // Arrange - Password shorter than min 8 characters
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    "token-uuid-1234", "john_doe", "123"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).completeStaffRegistration(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when invitation token is invalid")
        void completeStaffRegistration_Returns400BadRequest_WhenTokenInvalid() throws Exception {
            // Arrange
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    "invalid-token", "john_doe", "SecurePassword123"
            );
            doThrow(new InvalidTokenException("Invalid or expired invitation token."))
                    .when(authService).completeStaffRegistration(any(CompleteStaffRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Token"))
                    .andExpect(jsonPath("$.detail").value("Invalid or expired invitation token."));
        }

        @Test
        @DisplayName("Should return 409 Conflict when username already exists")
        void completeStaffRegistration_Returns409Conflict_WhenUsernameTaken() throws Exception {
            // Arrange
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    "token-uuid-1234", "taken_username", "SecurePassword123"
            );
            doThrow(new UsernameAlreadyExistsException("Username 'taken_username' is already taken."))
                    .when(authService).completeStaffRegistration(any(CompleteStaffRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Username Already Exists"))
                    .andExpect(jsonPath("$.detail").value("Username 'taken_username' is already taken."));
        }
    }
}
