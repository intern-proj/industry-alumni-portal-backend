package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.CompleteStaffRegistrationRequest;
import com.nsbm.authservice.dto.EmailNotificationMessage;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.entity.ManagementStaff;
import com.nsbm.authservice.entity.PendingStaff;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.exception.InvalidTokenException;
import com.nsbm.authservice.exception.StaffAlreadyExistsException;
import com.nsbm.authservice.exception.UsernameAlreadyExistsException;
import com.nsbm.authservice.repository.ManagementStaffRepository;
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
}
