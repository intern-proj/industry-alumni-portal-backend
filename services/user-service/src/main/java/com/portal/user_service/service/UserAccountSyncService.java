package com.portal.user_service.service;

import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserProfile;
import com.portal.user_service.model.UserRole;
import com.portal.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountSyncService implements CommandLineRunner {

    private final UserProfileRepository userProfileRepository;

    @Value("${app.auth-db.url:jdbc:postgresql://localhost:5432/auth_db}")
    private String authDbUrl;

    @Value("${app.auth-db.username:user}")
    private String authDbUsername;

    @Value("${app.auth-db.password:root}")
    private String authDbPassword;

    @Override
    public void run(String... args) {
        syncAllAccountsFromAuthService();
    }

    /**
     * Periodically syncs every 60 seconds to ensure any new staff, partner, or student registered
     * in auth-service automatically has a UserProfile in user-service.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void scheduledSync() {
        syncAllAccountsFromAuthService();
    }

    public synchronized void syncAllAccountsFromAuthService() {
        try (Connection conn = DriverManager.getConnection(authDbUrl, authDbUsername, authDbPassword)) {
            syncManagementStaff(conn);
            syncIndustryPartners(conn);
            syncStudents(conn);
        } catch (Exception e) {
            log.warn("[UserAccountSyncService] Could not complete sync from auth_db: {}", e.getMessage());
        }
    }

    private void syncManagementStaff(Connection conn) {
        String sql = "SELECT username, email, role FROM management_staff";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String roleStr = rs.getString("role");

                if (email == null || email.isBlank()) continue;

                if (!userProfileRepository.existsByEmail(email)) {
                    UserRole role = UserRole.ADMINISTRATIVE_STAFF;
                    try {
                        role = UserRole.valueOf(roleStr);
                    } catch (Exception ignored) {}

                    UserProfile profile = UserProfile.builder()
                            .userId(username != null ? username : email)
                            .firstName(username != null ? username : "Staff")
                            .lastName("")
                            .email(email)
                            .userRole(role)
                            .accountStatus(AccountStatus.ACTIVE)
                            .faculty("Academic & Institutional Operations")
                            .department("University Administration")
                            .isActivelyLooking(false)
                            .build();

                    userProfileRepository.save(profile);
                    log.info("[UserAccountSyncService] Synced staff member into user_profiles: {} ({})", email, role);
                }
            }
        } catch (Exception e) {
            log.warn("[UserAccountSyncService] Error syncing management_staff: {}", e.getMessage());
        }
    }

    private void syncIndustryPartners(Connection conn) {
        String sql = "SELECT username, email, company_name, representative_full_name, phone, logo_url, company_industry, account_status FROM industry_partners";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String companyName = rs.getString("company_name");
                String repName = rs.getString("representative_full_name");
                String phone = rs.getString("phone");
                String logoUrl = rs.getString("logo_url");
                String industry = rs.getString("company_industry");
                String statusStr = rs.getString("account_status");

                if (email == null || email.isBlank()) continue;

                if (!userProfileRepository.existsByEmail(email)) {
                    AccountStatus status = "INACTIVE".equalsIgnoreCase(statusStr) ? AccountStatus.INACTIVE : AccountStatus.ACTIVE;

                    UserProfile profile = UserProfile.builder()
                            .userId(username != null ? username : email)
                            .firstName(repName != null && !repName.isBlank() ? repName : (companyName != null ? companyName : "Partner"))
                            .lastName(companyName != null && !companyName.equals(repName) ? "(" + companyName + ")" : "")
                            .email(email)
                            .phone(phone)
                            .profilePicUrl(logoUrl)
                            .userRole(UserRole.INDUSTRY_PARTNER)
                            .accountStatus(status)
                            .faculty(industry != null && !industry.isBlank() ? industry : "Corporate & Industry Relations")
                            .isActivelyLooking(false)
                            .build();

                    userProfileRepository.save(profile);
                    log.info("[UserAccountSyncService] Synced industry partner into user_profiles: {} ({})", companyName, email);
                }
            }
        } catch (Exception e) {
            log.warn("[UserAccountSyncService] Error syncing industry_partners: {}", e.getMessage());
        }
    }

    private void syncStudents(Connection conn) {
        String sql = "SELECT username, email FROM students";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");

                if (email == null || email.isBlank()) continue;

                if (!userProfileRepository.existsByEmail(email)) {
                    UserProfile profile = UserProfile.builder()
                            .userId(username != null ? username : email)
                            .firstName(username != null ? username : "Student")
                            .lastName("")
                            .email(email)
                            .userRole(UserRole.STUDENT)
                            .accountStatus(AccountStatus.ACTIVE)
                            .faculty("Faculty of Computing")
                            .isActivelyLooking(true)
                            .build();

                    userProfileRepository.save(profile);
                    log.info("[UserAccountSyncService] Synced student into user_profiles: {} ({})", email, username);
                }
            }
        } catch (Exception e) {
            log.warn("[UserAccountSyncService] Error syncing students: {}", e.getMessage());
        }
    }
}
