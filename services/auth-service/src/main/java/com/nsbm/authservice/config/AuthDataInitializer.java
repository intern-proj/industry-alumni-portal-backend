package com.nsbm.authservice.config;

import com.nsbm.authservice.entity.ManagementStaff;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.entity.Student;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import com.nsbm.authservice.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthDataInitializer.class);

    private final ManagementStaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username:admin}")
    private String adminUsername;

    @Value("${app.admin.default-password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.default-email:prasadkvithana@gmail.com}")
    private String adminEmail;

    public AuthDataInitializer(ManagementStaffRepository staffRepository, 
                               StudentRepository studentRepository, 
                               PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Starting AuthDataInitializer...");

        // 1. Initialize Admin
        if (!staffRepository.existsByUsername(adminUsername) && !staffRepository.existsByEmail(adminEmail)) {
            ManagementStaff admin = ManagementStaff.builder()
                    .username(adminUsername)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .role(Role.SYSTEM_ADMIN)
                    .build();

            staffRepository.save(admin);
            log.info("Initial admin account created with username: '{}' and email: '{}'", adminUsername, adminEmail);
        } else {
            log.info("Admin account already exists. Skipping.");
        }

        // 2. Initialize 10 Students
        if (studentRepository.count() == 0) {
            log.info("No students found. Seeding 10 default students...");
            List<Student> students = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                String studentUsername = "student" + i;
                students.add(Student.builder()
                        .username(studentUsername)
                        .passwordHash(passwordEncoder.encode("Student@" + i))
                        .email(studentUsername + "@students.nsbm.ac.lk")
                        .build());
            }
            studentRepository.saveAll(students);
            log.info("Successfully seeded 10 students.");
        } else {
            log.info("Students already exist. Skipping student seeding.");
        }
    }
}
