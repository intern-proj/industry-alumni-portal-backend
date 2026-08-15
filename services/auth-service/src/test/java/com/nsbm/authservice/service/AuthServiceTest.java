package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.*;
import com.nsbm.authservice.entity.*;
import com.nsbm.authservice.exception.InvalidTokenException;
import com.nsbm.authservice.exception.StaffAlreadyExistsException;
import com.nsbm.authservice.exception.UsernameAlreadyExistsException;
import com.nsbm.authservice.repository.IndustryPartnerRepository;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import com.nsbm.authservice.repository.PendingPartnerRepository;
import com.nsbm.authservice.repository.PendingStaffRepository;
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
    private IndustryPartnerRepository partnerRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "exchange", "notification.exchange");
        ReflectionTestUtils.setField(authService, "routingKey", "notification.routingkey");
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
            ArgumentCaptor<EmailNotificationMessage> messageCaptor = ArgumentCaptor.forClass(EmailNotificationMessage.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.routingkey"), messageCaptor.capture());
            EmailNotificationMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.recipientEmail()).isEqualTo("lecturer@nsbm.ac.lk");
            assertThat(sentMessage.eventType()).isEqualTo("STAFF_INVITATION");
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
            ArgumentCaptor<EmailNotificationMessage> messageCaptor = ArgumentCaptor.forClass(EmailNotificationMessage.class);
            verify(rabbitTemplate).convertAndSend(eq("notification.exchange"), eq("notification.routingkey"), messageCaptor.capture());
            EmailNotificationMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.recipientEmail()).isEqualTo("jane@company.com");
            assertThat(sentMessage.eventType()).isEqualTo("PARTNER_REGISTRATION");
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
}
