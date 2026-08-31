package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.ApplyPartnerRegistrationRequest;
import com.nsbm.authservice.dto.AuthResponse;
import com.nsbm.authservice.dto.CompletePartnerRegistrationRequest;
import com.nsbm.authservice.dto.CompleteStaffRegistrationRequest;
import com.nsbm.authservice.dto.ForgotPasswordRequest;
import com.nsbm.authservice.dto.LoginRequest;
import com.nsbm.authservice.dto.LoginResponse;
import com.nsbm.authservice.dto.OtpEmailPayload;
import com.nsbm.authservice.dto.OtpVerificationRequest;
import com.nsbm.authservice.dto.ResetPasswordRequest;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.dto.Step1LoginResponse;
import com.nsbm.authservice.dto.TokenValidationResponse;
import com.nsbm.authservice.dto.UpdateEmailPayload;
import com.nsbm.authservice.entity.*;
import com.nsbm.authservice.exception.*;
import com.nsbm.authservice.repository.*;
import com.nsbm.authservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ManagementStaffRepository staffRepository;

    @Mock
    private PendingStaffRepository pendingStaffRepository;

    @Mock
    private PendingPartnerRepository pendingPartnerRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private IndustryPartnerRepository partnerRepository;

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "exchange", "notification.exchange");
        ReflectionTestUtils.setField(authService, "routingKey", "notification.routingkey");
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 5L);
        ReflectionTestUtils.setField(authService, "resetPasswordExpirationMinutes", 15L);
        ReflectionTestUtils.setField(authService, "resetPasswordFrontendUrl", "https://portal.domain.com/reset-password");
    }

    @Nested
    @DisplayName("inviteStaff Tests")
    class InviteStaffTests {

        @Test
        @DisplayName("Should successfully invite staff when email does not exist")
        void inviteStaff_Success() {
            // Arrange
            StaffInvitationRequest request = new StaffInvitationRequest("lecturer@nsbm.ac.lk", Role.ACADEMIC_STAFF);
            when(staffRepository.existsByEmail(request.email())).thenReturn(false);
            when(pendingStaffRepository.existsByEmail(request.email())).thenReturn(false);

            // Act
            authService.inviteStaff(request);

            // Assert - Verify PendingStaff entity saved
            ArgumentCaptor<PendingStaff> pendingStaffCaptor = ArgumentCaptor.forClass(PendingStaff.class);
            verify(pendingStaffRepository).save(pendingStaffCaptor.capture());
            PendingStaff savedPending = pendingStaffCaptor.getValue();
            assertThat(savedPending.getEmail()).isEqualTo("lecturer@nsbm.ac.lk");
            assertThat(savedPending.getRole()).isEqualTo(Role.ACADEMIC_STAFF);
            assertThat(savedPending.getInvitationToken()).isNotBlank();

            // Assert - Verify RabbitMQ message published
            ArgumentCaptor<UpdateEmailPayload> messageCaptor = ArgumentCaptor.forClass(UpdateEmailPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.routingkey"), messageCaptor.capture());
            UpdateEmailPayload sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.toEmail()).isEqualTo("lecturer@nsbm.ac.lk");
            assertThat(sentMessage.updateType()).isEqualTo("GENERAL_UPDATE");
        }

        @Test
        @DisplayName("Should throw StaffAlreadyExistsException when email already registered as ManagementStaff")
        void inviteStaff_ThrowsException_WhenEmailExistsInStaff() {
            // Arrange
            StaffInvitationRequest request = new StaffInvitationRequest("existing@nsbm.ac.lk", Role.ADMIN);
            when(staffRepository.existsByEmail(request.email())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.inviteStaff(request))
                    .isInstanceOf(StaffAlreadyExistsException.class)
                    .hasMessageContaining("existing@nsbm.ac.lk");

            verify(pendingStaffRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("Should throw StaffAlreadyExistsException when email already exists in PendingStaff")
        void inviteStaff_ThrowsException_WhenEmailExistsInPendingStaff() {
            // Arrange
            StaffInvitationRequest request = new StaffInvitationRequest("pending@nsbm.ac.lk", Role.FACULTY_COORDINATOR);
            when(staffRepository.existsByEmail(request.email())).thenReturn(false);
            when(pendingStaffRepository.existsByEmail(request.email())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.inviteStaff(request))
                    .isInstanceOf(StaffAlreadyExistsException.class)
                    .hasMessageContaining("pending@nsbm.ac.lk");

            verify(pendingStaffRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("completeStaffRegistration Tests")
    class CompleteStaffRegistrationTests {

        @Test
        @DisplayName("Should successfully complete staff registration with valid token")
        void completeStaffRegistration_Success() {
            // Arrange
            String token = "valid-token-123";
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    token, "john_doe", "SecurePassword123"
            );

            PendingStaff pendingStaff = PendingStaff.builder()
                    .id(1L)
                    .email("john@nsbm.ac.lk")
                    .role(Role.ACADEMIC_STAFF)
                    .invitationToken(token)
                    .build();

            when(pendingStaffRepository.findByInvitationToken(token)).thenReturn(Optional.of(pendingStaff));
            when(staffRepository.existsByUsername("john_doe")).thenReturn(false);
            when(passwordEncoder.encode("SecurePassword123")).thenReturn("encodedPasswordHash");

            // Act
            authService.completeStaffRegistration(request);

            // Assert - Verify ManagementStaff created and saved
            ArgumentCaptor<ManagementStaff> staffCaptor = ArgumentCaptor.forClass(ManagementStaff.class);
            verify(staffRepository).save(staffCaptor.capture());
            ManagementStaff savedStaff = staffCaptor.getValue();
            assertThat(savedStaff.getUsername()).isEqualTo("john_doe");
            assertThat(savedStaff.getEmail()).isEqualTo("john@nsbm.ac.lk");
            assertThat(savedStaff.getRole()).isEqualTo(Role.ACADEMIC_STAFF);
            assertThat(savedStaff.getPasswordHash()).isEqualTo("encodedPasswordHash");

            // Assert - Verify PendingStaff record removed
            verify(pendingStaffRepository).delete(pendingStaff);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when invitation token is invalid or expired")
        void completeStaffRegistration_ThrowsException_WhenTokenInvalid() {
            // Arrange
            String invalidToken = "invalid-token-999";
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    invalidToken, "john_doe", "SecurePassword123"
            );

            when(pendingStaffRepository.findByInvitationToken(invalidToken)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.completeStaffRegistration(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired invitation token.");

            verify(staffRepository, never()).save(any());
            verify(pendingStaffRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw UsernameAlreadyExistsException when username is already taken")
        void completeStaffRegistration_ThrowsException_WhenUsernameAlreadyExists() {
            // Arrange
            String token = "valid-token-123";
            CompleteStaffRegistrationRequest request = new CompleteStaffRegistrationRequest(
                    token, "existing_user", "SecurePassword123"
            );

            PendingStaff pendingStaff = PendingStaff.builder()
                    .id(1L)
                    .email("john@nsbm.ac.lk")
                    .role(Role.ACADEMIC_STAFF)
                    .invitationToken(token)
                    .build();

            when(pendingStaffRepository.findByInvitationToken(token)).thenReturn(Optional.of(pendingStaff));
            when(staffRepository.existsByUsername("existing_user")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.completeStaffRegistration(request))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining("existing_user");

            verify(staffRepository, never()).save(any());
            verify(pendingStaffRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("createPendingPartner Tests")
    class CreatePendingPartnerTests {

        @Test
        @DisplayName("Should successfully create pending partner when email does not exist")
        void createPendingPartner_Success() {
            // Arrange
            ApplyPartnerRegistrationRequest request = new ApplyPartnerRegistrationRequest(
                    "Jane Smith", "jane@company.com", "+94771234567",
                    "HR Director", "TechCorp Ltd", "IT Services",
                    "123 Business Way, Colombo", "Leading software company"
            );
            when(pendingPartnerRepository.existsByEmail(request.email())).thenReturn(false);

            // Act
            authService.createPendingPartner(request);

            // Assert - Verify PendingPartner entity saved
            ArgumentCaptor<PendingPartner> partnerCaptor = ArgumentCaptor.forClass(PendingPartner.class);
            verify(pendingPartnerRepository).save(partnerCaptor.capture());
            PendingPartner savedPartner = partnerCaptor.getValue();
            assertThat(savedPartner.getRepresentativeFullName()).isEqualTo("Jane Smith");
            assertThat(savedPartner.getEmail()).isEqualTo("jane@company.com");
            assertThat(savedPartner.getCompanyName()).isEqualTo("TechCorp Ltd");
            assertThat(savedPartner.getRegistrationToken()).isNotBlank();

            // Assert - Verify RabbitMQ message published
            ArgumentCaptor<UpdateEmailPayload> messageCaptor = ArgumentCaptor.forClass(UpdateEmailPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.routingkey"), messageCaptor.capture());
            UpdateEmailPayload sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.toEmail()).isEqualTo("jane@company.com");
            assertThat(sentMessage.updateType()).isEqualTo("GENERAL_UPDATE");
        }

        @Test
        @DisplayName("Should throw StaffAlreadyExistsException when partner email already exists in pending partners")
        void createPendingPartner_ThrowsException_WhenEmailExists() {
            // Arrange
            ApplyPartnerRegistrationRequest request = new ApplyPartnerRegistrationRequest(
                    "Jane Smith", "existing@company.com", "+94771234567",
                    "HR Director", "TechCorp Ltd", "IT Services",
                    "123 Business Way, Colombo", "Leading software company"
            );
            when(pendingPartnerRepository.existsByEmail(request.email())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.createPendingPartner(request))
                    .isInstanceOf(StaffAlreadyExistsException.class)
                    .hasMessageContaining("existing@company.com");

            verify(pendingPartnerRepository, never()).save(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("completePartnerRegistration Tests")
    class CompletePartnerRegistrationTests {

        @Test
        @DisplayName("Should successfully complete partner registration with valid token")
        void completePartnerRegistration_Success() {
            // Arrange
            String token = "partner-token-789";
            CompletePartnerRegistrationRequest request = new CompletePartnerRegistrationRequest(
                    token, "techcorp_admin", "PartnerPassword123"
            );

            PendingPartner pendingPartner = PendingPartner.builder()
                    .id(2L)
                    .representativeFullName("Jane Smith")
                    .email("jane@company.com")
                    .phone("+94771234567")
                    .representativeJobRole("HR Director")
                    .companyName("TechCorp Ltd")
                    .companyIndustry("IT Services")
                    .companyAddress("123 Business Way, Colombo")
                    .companyDescription("Leading software company")
                    .registrationToken(token)
                    .build();

            when(pendingPartnerRepository.findByRegistrationToken(token)).thenReturn(Optional.of(pendingPartner));
            when(passwordEncoder.encode("PartnerPassword123")).thenReturn("hashedPartnerPassword");

            // Act
            authService.completePartnerRegistration(request);

            // Assert - Verify IndustryPartner entity created and saved
            ArgumentCaptor<IndustryPartner> partnerCaptor = ArgumentCaptor.forClass(IndustryPartner.class);
            verify(partnerRepository).save(partnerCaptor.capture());
            IndustryPartner savedPartner = partnerCaptor.getValue();
            assertThat(savedPartner.getUsername()).isEqualTo("techcorp_admin");
            assertThat(savedPartner.getEmail()).isEqualTo("jane@company.com");
            assertThat(savedPartner.getCompanyName()).isEqualTo("TechCorp Ltd");
            assertThat(savedPartner.getPasswordHash()).isEqualTo("hashedPartnerPassword");

            // Assert - Verify PendingPartner record deleted
            verify(pendingPartnerRepository).delete(pendingPartner);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when partner registration token is invalid")
        void completePartnerRegistration_ThrowsException_WhenTokenInvalid() {
            // Arrange
            String invalidToken = "invalid-token-000";
            CompletePartnerRegistrationRequest request = new CompletePartnerRegistrationRequest(
                    invalidToken, "techcorp_admin", "PartnerPassword123"
            );

            when(pendingPartnerRepository.findByRegistrationToken(invalidToken)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.completePartnerRegistration(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired registration token.");

            verify(partnerRepository, never()).save(any());
            verify(pendingPartnerRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully authenticate Student directly and return LoginResponse")
        void login_Student_Success() {
            // Arrange
            LoginRequest request = new LoginRequest("student_user", "password123");
            Student student = Student.builder()
                    .id(1L)
                    .username("student_user")
                    .email("student@nsbm.ac.lk")
                    .passwordHash("encoded_pass")
                    .build();

            when(studentRepository.findByUsername("student_user")).thenReturn(Optional.of(student));
            when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
            when(jwtTokenProvider.generateToken("student_user", "student@nsbm.ac.lk", "STUDENT", "STUDENT"))
                    .thenReturn("mock-jwt-token-student");

            // Act
            LoginResponse response = authService.login(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.requiresOtp()).isFalse();
            assertThat(response.accessToken()).isEqualTo("mock-jwt-token-student");
            assertThat(response.username()).isEqualTo("student_user");
            assertThat(response.role()).isEqualTo("STUDENT");
            assertThat(response.userType()).isEqualTo("STUDENT");
        }

        @Test
        @DisplayName("Should initiate 2FA OTP for IndustryPartner and return LoginResponse with requiresOtp true")
        void login_Partner_Success() {
            // Arrange
            LoginRequest request = new LoginRequest("partner_user", "password123");
            IndustryPartner partner = IndustryPartner.builder()
                    .id(1L)
                    .username("partner_user")
                    .email("partner@company.com")
                    .passwordHash("encoded_pass")
                    .build();

            when(studentRepository.findByUsername("partner_user")).thenReturn(Optional.empty());
            when(staffRepository.findByUsername("partner_user")).thenReturn(Optional.empty());
            when(partnerRepository.findByUsername("partner_user")).thenReturn(Optional.of(partner));
            when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

            // Act
            LoginResponse response = authService.login(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.requiresOtp()).isTrue();
            assertThat(response.sessionToken()).isNotBlank();
            assertThat(response.username()).isEqualTo("partner_user");
            verify(otpCodeRepository).save(any(OtpCode.class));
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.otp"), any(OtpEmailPayload.class));
        }

        @Test
        @DisplayName("Should initiate 2FA OTP for Academic Staff and return LoginResponse with requiresOtp true")
        void login_AcademicStaff_Success() {
            // Arrange
            LoginRequest request = new LoginRequest("academic_staff", "password123");
            ManagementStaff staff = ManagementStaff.builder()
                    .id(1L)
                    .username("academic_staff")
                    .email("academic@nsbm.ac.lk")
                    .role(Role.ACADEMIC_STAFF)
                    .passwordHash("encoded_pass")
                    .build();

            when(studentRepository.findByUsername("academic_staff")).thenReturn(Optional.empty());
            when(staffRepository.findByUsername("academic_staff")).thenReturn(Optional.of(staff));
            when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

            // Act
            LoginResponse response = authService.login(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.requiresOtp()).isTrue();
            assertThat(response.sessionToken()).isNotBlank();
            assertThat(response.username()).isEqualTo("academic_staff");
            verify(otpCodeRepository).save(any(OtpCode.class));
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.otp"), any(OtpEmailPayload.class));
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when credentials do not match")
        void login_ThrowsException_WhenInvalidCredentials() {
            // Arrange
            LoginRequest request = new LoginRequest("unknown_user", "wrong_pass");
            when(studentRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());
            when(staffRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());
            when(partnerRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid username or password.");
        }
    }

    @Nested
    @DisplayName("initiateStaffLogin Tests")
    class InitiateStaffLoginTests {

        @Test
        @DisplayName("Should successfully generate OTP, save OtpCode and send RabbitMQ notification")
        void initiateStaffLogin_Success() {
            // Arrange
            LoginRequest request = new LoginRequest("admin_staff", "AdminPassword123");
            ManagementStaff staff = ManagementStaff.builder()
                    .id(1L)
                    .username("admin_staff")
                    .email("admin@nsbm.ac.lk")
                    .role(Role.ADMIN)
                    .passwordHash("hashedAdminPassword")
                    .build();

            when(staffRepository.findByUsername("admin_staff")).thenReturn(Optional.of(staff));
            when(passwordEncoder.matches("AdminPassword123", "hashedAdminPassword")).thenReturn(true);

            // Act
            Step1LoginResponse response = authService.initiateStaffLogin(request);

            // Assert - Verify OtpCode entity saved
            ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
            verify(otpCodeRepository).save(otpCaptor.capture());
            OtpCode savedOtp = otpCaptor.getValue();
            assertThat(savedOtp.getUsername()).isEqualTo("admin_staff");
            assertThat(savedOtp.getCode()).containsPattern("^\\d{6}$");
            assertThat(savedOtp.getSessionToken()).isNotBlank();

            // Assert - Verify RabbitMQ message published
            ArgumentCaptor<OtpEmailPayload> msgCaptor = ArgumentCaptor.forClass(OtpEmailPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.otp"), msgCaptor.capture());
            assertThat(msgCaptor.getValue().toEmail()).isEqualTo("admin@nsbm.ac.lk");

            // Assert - Verify returned Step1LoginResponse
            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo("admin_staff");
            assertThat(response.sessionToken()).isNotBlank();
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when staff password is wrong")
        void initiateStaffLogin_ThrowsException_WhenPasswordInvalid() {
            // Arrange
            LoginRequest request = new LoginRequest("admin_staff", "WrongPassword");
            ManagementStaff staff = ManagementStaff.builder()
                    .username("admin_staff")
                    .role(Role.ADMIN)
                    .passwordHash("hashedAdminPassword")
                    .build();

            when(staffRepository.findByUsername("admin_staff")).thenReturn(Optional.of(staff));
            when(passwordEncoder.matches("WrongPassword", "hashedAdminPassword")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.initiateStaffLogin(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid username or password.");

            verify(otpCodeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyStaffOtp Tests")
    class VerifyStaffOtpTests {

        @Test
        @DisplayName("Should successfully verify OTP and return AuthResponse with JWT token")
        void verifyStaffOtp_Success() {
            // Arrange
            OtpVerificationRequest request = new OtpVerificationRequest("session-token-123", "123456");
            OtpCode otpCode = OtpCode.builder()
                    .id(1L)
                    .username("admin_staff")
                    .code("123456")
                    .sessionToken("session-token-123")
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            ManagementStaff staff = ManagementStaff.builder()
                    .username("admin_staff")
                    .email("admin@nsbm.ac.lk")
                    .role(Role.ADMIN)
                    .build();

            when(otpCodeRepository.findTopBySessionTokenAndCodeOrderByCreatedAtDesc("session-token-123", "123456"))
                    .thenReturn(Optional.of(otpCode));
            when(staffRepository.findByUsername("admin_staff")).thenReturn(Optional.of(staff));
            when(jwtTokenProvider.generateToken("admin_staff", "admin@nsbm.ac.lk", "ADMIN", "MANAGEMENT_STAFF"))
                    .thenReturn("jwt-token-staff");

            // Act
            AuthResponse response = authService.verifyStaffOtp(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("jwt-token-staff");
            assertThat(response.role()).isEqualTo("ADMIN");
            assertThat(response.userType()).isEqualTo("MANAGEMENT_STAFF");
            verify(otpCodeRepository).delete(otpCode);
        }

        @Test
        @DisplayName("Should throw OtpInvalidException when OTP entity is expired")
        void verifyStaffOtp_ThrowsException_WhenOtpExpired() {
            // Arrange
            OtpVerificationRequest request = new OtpVerificationRequest("session-token-123", "123456");
            OtpCode expiredOtp = OtpCode.builder()
                    .id(1L)
                    .username("admin_staff")
                    .code("123456")
                    .sessionToken("session-token-123")
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired
                    .build();

            when(otpCodeRepository.findTopBySessionTokenAndCodeOrderByCreatedAtDesc("session-token-123", "123456"))
                    .thenReturn(Optional.of(expiredOtp));

            // Act & Assert
            assertThatThrownBy(() -> authService.verifyStaffOtp(request))
                    .isInstanceOf(OtpInvalidException.class)
                    .hasMessageContaining("OTP code has expired");

            verify(otpCodeRepository).delete(expiredOtp);
        }
    }

    @Nested
    @DisplayName("validateToken Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return TokenValidationResponse with true when JWT is valid")
        void validateToken_ValidToken() {
            // Arrange
            String token = "valid-jwt-token";
            when(jwtTokenProvider.validateToken(token)).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn("john");
            when(jwtTokenProvider.getEmailFromToken(token)).thenReturn("john@nsbm.ac.lk");
            when(jwtTokenProvider.getRoleFromToken(token)).thenReturn("STUDENT");
            when(jwtTokenProvider.getUserTypeFromToken(token)).thenReturn("STUDENT");

            // Act
            TokenValidationResponse response = authService.validateToken(token);

            // Assert
            assertThat(response.valid()).isTrue();
            assertThat(response.username()).isEqualTo("john");
            assertThat(response.role()).isEqualTo("STUDENT");
        }

        @Test
        @DisplayName("Should return TokenValidationResponse with false when JWT is invalid")
        void validateToken_InvalidToken() {
            // Arrange
            String token = "invalid-token";
            when(jwtTokenProvider.validateToken(token)).thenReturn(false);

            // Act
            TokenValidationResponse response = authService.validateToken(token);

            // Assert
            assertThat(response.valid()).isFalse();
            assertThat(response.username()).isNull();
        }
    }

    @Nested
    @DisplayName("forgotPassword Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should successfully create reset token and send notification for valid non-admin staff email")
        void forgotPassword_Staff_Success() {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("staff@nsbm.ac.lk");
            ManagementStaff staff = ManagementStaff.builder()
                    .id(1L)
                    .email("staff@nsbm.ac.lk")
                    .role(Role.FACULTY_COORDINATOR)
                    .build();

            when(studentRepository.findByEmail("staff@nsbm.ac.lk")).thenReturn(Optional.empty());
            when(staffRepository.findByEmail("staff@nsbm.ac.lk")).thenReturn(Optional.of(staff));

            // Act
            authService.forgotPassword(request);

            // Assert - Verify existing tokens deleted
            verify(passwordResetTokenRepository).deleteByEmail("staff@nsbm.ac.lk");

            // Assert - Verify PasswordResetToken saved
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            PasswordResetToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getEmail()).isEqualTo("staff@nsbm.ac.lk");
            assertThat(savedToken.getUserType()).isEqualTo("MANAGEMENT_STAFF");
            assertThat(savedToken.getToken()).isNotBlank();

            // Assert - Verify RabbitMQ notification sent
            ArgumentCaptor<UpdateEmailPayload> messageCaptor = ArgumentCaptor.forClass(UpdateEmailPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.routingkey"), messageCaptor.capture());
            UpdateEmailPayload sentMsg = messageCaptor.getValue();
            assertThat(sentMsg.toEmail()).isEqualTo("staff@nsbm.ac.lk");
            assertThat(sentMsg.updateType()).isEqualTo("GENERAL_UPDATE");
        }

        @Test
        @DisplayName("Should successfully create reset token and send notification for valid partner email")
        void forgotPassword_Partner_Success() {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("partner@company.com");
            IndustryPartner partner = IndustryPartner.builder()
                    .id(1L)
                    .email("partner@company.com")
                    .build();

            when(studentRepository.findByEmail("partner@company.com")).thenReturn(Optional.empty());
            when(staffRepository.findByEmail("partner@company.com")).thenReturn(Optional.empty());
            when(partnerRepository.findByEmail("partner@company.com")).thenReturn(Optional.of(partner));

            // Act
            authService.forgotPassword(request);

            // Assert
            verify(passwordResetTokenRepository).deleteByEmail("partner@company.com");
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUserType()).isEqualTo("INDUSTRY_PARTNER");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email belongs to a student")
        void forgotPassword_ThrowsException_ForStudent() {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("student@nsbm.ac.lk");
            Student student = Student.builder().email("student@nsbm.ac.lk").build();
            when(studentRepository.findByEmail("student@nsbm.ac.lk")).thenReturn(Optional.of(student));

            // Act & Assert
            assertThatThrownBy(() -> authService.forgotPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Forgot password feature is not available for students.");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email belongs to an Admin staff")
        void forgotPassword_ThrowsException_ForAdmin() {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("admin@nsbm.ac.lk");
            ManagementStaff adminStaff = ManagementStaff.builder()
                    .email("admin@nsbm.ac.lk")
                    .role(Role.ADMIN)
                    .build();
            when(studentRepository.findByEmail("admin@nsbm.ac.lk")).thenReturn(Optional.empty());
            when(staffRepository.findByEmail("admin@nsbm.ac.lk")).thenReturn(Optional.of(adminStaff));

            // Act & Assert
            assertThatThrownBy(() -> authService.forgotPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Forgot password feature is not available for admins.");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when email is not found")
        void forgotPassword_ThrowsException_WhenEmailNotFound() {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@nsbm.ac.lk");
            when(studentRepository.findByEmail("unknown@nsbm.ac.lk")).thenReturn(Optional.empty());
            when(staffRepository.findByEmail("unknown@nsbm.ac.lk")).thenReturn(Optional.empty());
            when(partnerRepository.findByEmail("unknown@nsbm.ac.lk")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.forgotPassword(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("No user account found with the provided email address.");
        }
    }

    @Nested
    @DisplayName("resetPassword Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should successfully reset password for staff member with valid token")
        void resetPassword_Staff_Success() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("reset-token-123", "NewPassword123", "NewPassword123");
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .id(1L)
                    .email("staff@nsbm.ac.lk")
                    .token("reset-token-123")
                    .userType("MANAGEMENT_STAFF")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            ManagementStaff staff = ManagementStaff.builder()
                    .id(1L)
                    .email("staff@nsbm.ac.lk")
                    .role(Role.FACULTY_COORDINATOR)
                    .build();

            when(passwordResetTokenRepository.findByToken("reset-token-123")).thenReturn(Optional.of(resetToken));
            when(staffRepository.findByEmail("staff@nsbm.ac.lk")).thenReturn(Optional.of(staff));
            when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPassword");

            // Act
            authService.resetPassword(request);

            // Assert
            verify(staffRepository).save(staff);
            assertThat(staff.getPasswordHash()).isEqualTo("newHashedPassword");
            verify(passwordResetTokenRepository).delete(resetToken);
        }

        @Test
        @DisplayName("Should successfully reset password for industry partner with valid token")
        void resetPassword_Partner_Success() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("partner-reset-token", "NewPartnerPass123", "NewPartnerPass123");
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .id(2L)
                    .email("partner@company.com")
                    .token("partner-reset-token")
                    .userType("INDUSTRY_PARTNER")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();

            IndustryPartner partner = IndustryPartner.builder()
                    .id(1L)
                    .email("partner@company.com")
                    .build();

            when(passwordResetTokenRepository.findByToken("partner-reset-token")).thenReturn(Optional.of(resetToken));
            when(partnerRepository.findByEmail("partner@company.com")).thenReturn(Optional.of(partner));
            when(passwordEncoder.encode("NewPartnerPass123")).thenReturn("newHashedPartnerPassword");

            // Act
            authService.resetPassword(request);

            // Assert
            verify(partnerRepository).save(partner);
            assertThat(partner.getPasswordHash()).isEqualTo("newHashedPartnerPassword");
            verify(passwordResetTokenRepository).delete(resetToken);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when passwords do not match")
        void resetPassword_ThrowsException_WhenPasswordsDoNotMatch() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("token-123", "NewPassword123", "MismatchPassword");

            // Act & Assert
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Passwords do not match.");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when reset token is invalid")
        void resetPassword_ThrowsException_WhenTokenInvalid() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "NewPassword123", "NewPassword123");
            when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired password reset token.");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when reset token is expired")
        void resetPassword_ThrowsException_WhenTokenExpired() {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewPassword123", "NewPassword123");
            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .id(1L)
                    .email("staff@nsbm.ac.lk")
                    .token("expired-token")
                    .userType("MANAGEMENT_STAFF")
                    .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                    .build();

            when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Password reset token has expired.");

            verify(passwordResetTokenRepository).delete(expiredToken);
        }
    }
}
