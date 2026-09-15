package com.nsbm.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.authservice.dto.ApplyPartnerRegistrationRequest;
import com.nsbm.authservice.dto.AuthResponse;
import com.nsbm.authservice.dto.CompletePartnerRegistrationRequest;
import com.nsbm.authservice.dto.CompleteStaffRegistrationRequest;
import com.nsbm.authservice.dto.CreateAdminRequest;
import com.nsbm.authservice.dto.ForgotPasswordRequest;
import com.nsbm.authservice.dto.LoginRequest;
import com.nsbm.authservice.dto.LoginResponse;
import com.nsbm.authservice.dto.OtpVerificationRequest;
import com.nsbm.authservice.dto.ResetPasswordRequest;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.dto.Step1LoginResponse;
import com.nsbm.authservice.dto.TokenValidationResponse;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.exception.GlobalExceptionHandler;
import com.nsbm.authservice.exception.InvalidCredentialsException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("POST /api/v1/auth/admin/create Tests")
    class CreateAdminApiTests {

        @Test
        @DisplayName("Should return 201 Created for valid admin create request")
        void createAdmin_Returns201Created() throws Exception {
            CreateAdminRequest request = new CreateAdminRequest("new_admin", "admin@nsbm.ac.lk", "SecurePass123!");
            doNothing().when(authService).createAdmin(any(CreateAdminRequest.class));

            mockMvc.perform(post("/api/v1/auth/admin/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(authService, times(1)).createAdmin(request);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/staff/invite Tests")
    class InviteStaffApiTests {

        @Test
        @DisplayName("Should return 201 Created for valid staff invitation request")
        void inviteStaff_Returns201Created() throws Exception {
            // Arrange
            StaffInvitationRequest request = new StaffInvitationRequest("lecturer@nsbm.ac.lk", Role.EVENT_COORDINATOR);
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
                        "role": "EVENT_COORDINATOR"
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
            StaffInvitationRequest request = new StaffInvitationRequest("existing@nsbm.ac.lk", Role.SYSTEM_ADMIN);
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
            when(authService.completeStaffRegistration(any(CompleteStaffRegistrationRequest.class))).thenReturn("staff@nsbm.ac.lk");

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

    @Nested
    @DisplayName("POST /api/v1/auth/partner/pending Tests")
    class CreatePendingPartnerApiTests {

        @Test
        @DisplayName("Should return 201 Created for valid apply partner registration request")
        void createPendingPartner_Returns201Created() throws Exception {
            // Arrange
            ApplyPartnerRegistrationRequest request = new ApplyPartnerRegistrationRequest(
                    "Jane Smith", "jane@company.com", "+94771234567",
                    "HR Director", "TechCorp Ltd", "IT Services",
                    "123 Business Way, Colombo", "Leading software company"
            );
            doNothing().when(authService).createPendingPartner(any(ApplyPartnerRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/partner/pending")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(authService, times(1)).createPendingPartner(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid format")
        void createPendingPartner_Returns400BadRequest_WhenInvalidEmail() throws Exception {
            // Arrange
            ApplyPartnerRegistrationRequest request = new ApplyPartnerRegistrationRequest(
                    "Jane Smith", "invalid-email-format", "+94771234567",
                    "HR Director", "TechCorp Ltd", "IT Services",
                    "123 Business Way, Colombo", "Leading software company"
            );

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/partner/pending")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).createPendingPartner(any());
        }

        @Test
        @DisplayName("Should return 409 Conflict when partner email already exists")
        void createPendingPartner_Returns409Conflict_WhenEmailExists() throws Exception {
            // Arrange
            ApplyPartnerRegistrationRequest request = new ApplyPartnerRegistrationRequest(
                    "Jane Smith", "existing@company.com", "+94771234567",
                    "HR Director", "TechCorp Ltd", "IT Services",
                    "123 Business Way, Colombo", "Leading software company"
            );
            doThrow(new StaffAlreadyExistsException("Partner with email existing@company.com is already registered or invited."))
                    .when(authService).createPendingPartner(any(ApplyPartnerRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/partner/pending")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Resource Conflict"))
                    .andExpect(jsonPath("$.detail").value("Partner with email existing@company.com is already registered or invited."));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/partner/complete-registration Tests")
    class CompletePartnerRegistrationApiTests {

        @Test
        @DisplayName("Should return 201 Created for valid complete partner registration request")
        void completePartnerRegistration_Returns201Created() throws Exception {
            // Arrange
            CompletePartnerRegistrationRequest request = new CompletePartnerRegistrationRequest(
                    "partner-token-1234", "techcorp_admin", "PartnerPassword123"
            );
            when(authService.completePartnerRegistration(any(CompletePartnerRegistrationRequest.class))).thenReturn("partner@nsbm.ac.lk");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/partner/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(authService, times(1)).completePartnerRegistration(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when registration token is invalid")
        void completePartnerRegistration_Returns400BadRequest_WhenTokenInvalid() throws Exception {
            // Arrange
            CompletePartnerRegistrationRequest request = new CompletePartnerRegistrationRequest(
                    "invalid-token", "techcorp_admin", "PartnerPassword123"
            );
            doThrow(new InvalidTokenException("Invalid or expired registration token."))
                    .when(authService).completePartnerRegistration(any(CompletePartnerRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/partner/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Token"))
                    .andExpect(jsonPath("$.detail").value("Invalid or expired registration token."));
        }

        @Test
        @DisplayName("Should return 409 Conflict when username already exists")
        void completePartnerRegistration_Returns409Conflict_WhenUsernameTaken() throws Exception {
            // Arrange
            CompletePartnerRegistrationRequest request = new CompletePartnerRegistrationRequest(
                    "partner-token-1234", "taken_username", "PartnerPassword123"
            );
            doThrow(new UsernameAlreadyExistsException("Username 'taken_username' is already taken."))
                    .when(authService).completePartnerRegistration(any(CompletePartnerRegistrationRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/partner/complete-registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Username Already Exists"))
                    .andExpect(jsonPath("$.detail").value("Username 'taken_username' is already taken."));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login Tests")
    class LoginStudentOrPartnerApiTests {

        @Test
        @DisplayName("Should return 200 OK with LoginResponse for student login")
        void loginStudentOrPartner_Returns200OK() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("student_user", "password123");
            AuthResponse auth = new AuthResponse("mock-jwt-token", "student_user", "student@nsbm.ac.lk", "STUDENT", "STUDENT");
            LoginResponse response = LoginResponse.direct(auth);
            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.username").value("student_user"))
                    .andExpect(jsonPath("$.role").value("STUDENT"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/staff/login Tests")
    class InitiateStaffLoginApiTests {

        @Test
        @DisplayName("Should return 200 OK with Step1LoginResponse for staff login step 1")
        void initiateStaffLogin_Returns200OK() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin_staff", "password123");
            Step1LoginResponse response = new Step1LoginResponse("session-token-123", "admin_staff", "OTP sent", 300L);
            when(authService.initiateAdminLogin(any(LoginRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionToken").value("session-token-123"))
                    .andExpect(jsonPath("$.username").value("admin_staff"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/staff/verify-otp Tests")
    class VerifyStaffOtpApiTests {

        @Test
        @DisplayName("Should return 200 OK with AuthResponse for valid staff OTP verification")
        void verifyStaffOtp_Returns200OK() throws Exception {
            // Arrange
            OtpVerificationRequest request = new OtpVerificationRequest("session-token-123", "123456");
            AuthResponse response = new AuthResponse("staff-jwt-token", "admin_staff", "admin@nsbm.ac.lk", "ADMIN", "MANAGEMENT_STAFF");
            when(authService.verifyOtp(any(OtpVerificationRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/staff/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("staff-jwt-token"))
                    .andExpect(jsonPath("$.userType").value("MANAGEMENT_STAFF"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password Tests")
    class ForgotPasswordApiTests {

        @Test
        @DisplayName("Should return 200 OK for valid forgot password request")
        void forgotPassword_Returns200OK() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("user@nsbm.ac.lk");
            doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).forgotPassword(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid")
        void forgotPassword_Returns400BadRequest_WhenInvalidEmail() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email-format");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).forgotPassword(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password Tests")
    class ResetPasswordApiTests {

        @Test
        @DisplayName("Should return 200 OK for valid reset password request")
        void resetPassword_Returns200OK() throws Exception {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("reset-token-123", "NewPassword123", "NewPassword123");
            doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).resetPassword(request);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is too short")
        void resetPassword_Returns400BadRequest_WhenShortPassword() throws Exception {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("reset-token-123", "12345", "12345");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).resetPassword(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/validate Tests")
    class ValidateTokenApiTests {

        @Test
        @DisplayName("Should return 200 OK with TokenValidationResponse for token param or header")
        void validateToken_Returns200OK() throws Exception {
            // Arrange
            TokenValidationResponse response = new TokenValidationResponse(true, "john", "john@nsbm.ac.lk", "STUDENT", "STUDENT");
            when(authService.validateToken("bearer-jwt-token")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/validate")
                            .header("Authorization", "Bearer bearer-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.username").value("john"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me Tests")
    class GetCurrentUserApiTests {

        @Test
        @DisplayName("Should return 200 OK with current user validation info")
        void getCurrentUser_Returns200OK() throws Exception {
            // Arrange
            TokenValidationResponse response = new TokenValidationResponse(true, "john", "john@nsbm.ac.lk", "STUDENT", "STUDENT");
            when(authService.validateToken("bearer-jwt-token")).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer bearer-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.username").value("john"));
        }
    }
}
