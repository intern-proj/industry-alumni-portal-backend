package com.nsbm.authservice.config;

import com.nsbm.authservice.entity.ManagementStaff;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private ManagementStaffRepository staffRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "Admin");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "admin@123");
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@nsbm.ac.lk");
    }

    @Test
    @DisplayName("Should create default admin account when admin username and email do not exist")
    void run_CreatesAdmin_WhenAdminDoesNotExist() {
        // Arrange
        when(staffRepository.existsByUsername("Admin")).thenReturn(false);
        when(staffRepository.existsByEmail("admin@nsbm.ac.lk")).thenReturn(false);
        when(passwordEncoder.encode("admin@123")).thenReturn("encoded_admin_pass");

        // Act
        adminInitializer.run();

        // Assert - Verify ManagementStaff saved with ADMIN role
        ArgumentCaptor<ManagementStaff> adminCaptor = ArgumentCaptor.forClass(ManagementStaff.class);
        verify(staffRepository).save(adminCaptor.capture());
        ManagementStaff savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getUsername()).isEqualTo("Admin");
        assertThat(savedAdmin.getEmail()).isEqualTo("admin@nsbm.ac.lk");
        assertThat(savedAdmin.getPasswordHash()).isEqualTo("encoded_admin_pass");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("Should skip creation when admin username already exists")
    void run_SkipsCreation_WhenAdminExistsByUsername() {
        // Arrange
        when(staffRepository.existsByUsername("Admin")).thenReturn(true);

        // Act
        adminInitializer.run();

        // Assert
        verify(staffRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip creation when admin email already exists")
    void run_SkipsCreation_WhenAdminExistsByEmail() {
        // Arrange
        when(staffRepository.existsByUsername("Admin")).thenReturn(false);
        when(staffRepository.existsByEmail("admin@nsbm.ac.lk")).thenReturn(true);

        // Act
        adminInitializer.run();

        // Assert
        verify(staffRepository, never()).save(any());
    }
}
