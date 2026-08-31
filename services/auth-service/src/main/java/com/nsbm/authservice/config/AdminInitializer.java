package com.nsbm.authservice.config;

import com.nsbm.authservice.entity.ManagementStaff;
import com.nsbm.authservice.entity.Role;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final ManagementStaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username:admin}")
    private String adminUsername;

    @Value("${app.admin.default-password:admin@123}")
    private String adminPassword;

    @Value("${app.admin.default-email:prasadkvithana@gmail.com}")
    private String adminEmail;

    public AdminInitializer(ManagementStaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
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
            log.info("Admin account with username '{}' or email '{}' already exists. Skipping initial creation.",
                    adminUsername, adminEmail);
        }
    }
}
