package com.nsbm.authservice.config;

import com.nsbm.authservice.entity.ManagementStaff;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.entity.Student;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import com.nsbm.authservice.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthDataInitializerTest {

    @Mock
    private ManagementStaffRepository staffRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthDataInitializer authDataInitializer;

    @Captor
    private ArgumentCaptor<List<Student>> studentListCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authDataInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(authDataInitializer, "adminPassword", "Admin@123");
        ReflectionTestUtils.setField(authDataInitializer, "adminEmail", "prasadkvithana@gmail.com");
    }

    @Test
    @DisplayName("Should create default admin account and students when none exist")
    void run_CreatesAdminAndStudents_WhenNoneExist() {
        // Arrange
        when(staffRepository.existsByUsername("admin")).thenReturn(false);
        when(staffRepository.existsByEmail("prasadkvithana@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");
        when(studentRepository.count()).thenReturn(0L);

        // Act
        authDataInitializer.run();

        // Assert - Verify Admin saved
        ArgumentCaptor<ManagementStaff> adminCaptor = ArgumentCaptor.forClass(ManagementStaff.class);
        verify(staffRepository).save(adminCaptor.capture());
        ManagementStaff savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getUsername()).isEqualTo("admin");
        assertThat(savedAdmin.getEmail()).isEqualTo("prasadkvithana@gmail.com");
        assertThat(savedAdmin.getPasswordHash()).isEqualTo("encoded_pass");
        assertThat(savedAdmin.getRole()).isEqualTo(Role.SYSTEM_ADMIN);

        // Assert - Verify Students saved
        verify(studentRepository).saveAll(studentListCaptor.capture());
        List<Student> savedStudents = studentListCaptor.getValue();
        assertThat(savedStudents).hasSize(10);
        assertThat(savedStudents.get(0).getUsername()).isEqualTo("student1");
    }

    @Test
    @DisplayName("Should skip creation when admin and students already exist")
    void run_SkipsCreation_WhenDataExists() {
        // Arrange
        when(staffRepository.existsByUsername("admin")).thenReturn(true);
        when(studentRepository.count()).thenReturn(10L);

        // Act
        authDataInitializer.run();

        // Assert
        verify(staffRepository, never()).save(any());
        verify(studentRepository, never()).saveAll(any());
    }
}
