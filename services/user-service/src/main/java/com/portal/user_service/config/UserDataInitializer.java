package com.portal.user_service.config;

import com.portal.user_service.model.UserProfile;
import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserRole;
import com.portal.user_service.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class UserDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    private final UserProfileRepository userProfileRepository;

    @Value("${app.admin.default-username:admin}")
    private String adminUsername;

    @Value("${app.admin.default-email:prasadkvithana@gmail.com}")
    private String adminEmail;

    public UserDataInitializer(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting UserDataInitializer...");

        // 1. Initialize Admin Profile
        if (!userProfileRepository.existsById(adminUsername)) {
            UserProfile adminProfile = UserProfile.builder()
                    .userId(adminUsername)
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .userRole(UserRole.SYSTEM_ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
                    .profilePicUrl("https://ui-avatars.com/api/?name=System+Admin&background=0D8ABC&color=fff")
                    .bio("Default System Administrator account.")
                    .build();
            userProfileRepository.save(adminProfile);
            log.info("Admin UserProfile created successfully.");
        }

        // 2. Initialize 10 Student Profiles
        if (userProfileRepository.count() <= 1) { // Only admin exists
            log.info("Seeding 10 student profiles...");
            List<UserProfile> students = new ArrayList<>();
            Random random = new Random();
            
            String[] firstNames = {"John", "Emma", "Michael", "Sophia", "William", "Olivia", "James", "Ava", "Alexander", "Mia"};
            String[] lastNames = {"Smith", "Johnson", "Brown", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin"};
            String[] faculties = {"Computing", "Business", "Engineering", "Science"};

            for (int i = 1; i <= 10; i++) {
                String studentUsername = "student" + i;
                String firstName = firstNames[i - 1];
                String lastName = lastNames[i - 1];
                int picId = random.nextInt(70) + 1; // Random number for image
                String gender = i % 2 == 0 ? "women" : "men";
                
                UserProfile student = UserProfile.builder()
                        .userId(studentUsername)
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(studentUsername + "@students.nsbm.ac.lk")
                        .userRole(UserRole.STUDENT)
                        .accountStatus(AccountStatus.ACTIVE)
                        .profilePicUrl("https://randomuser.me/api/portraits/" + gender + "/" + picId + ".jpg")
                        .bio("Aspiring software engineer currently studying at NSBM Green University.")
                        .faculty(faculties[random.nextInt(faculties.length)])
                        .isActivelyLooking(true)
                        .build();
                        
                students.add(student);
            }
            userProfileRepository.saveAll(students);
            log.info("Successfully seeded 10 student profiles.");
        }
    }
}
