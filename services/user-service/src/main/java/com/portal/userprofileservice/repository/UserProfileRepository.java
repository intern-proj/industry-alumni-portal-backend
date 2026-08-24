package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.AccountStatus;
import com.portal.userprofileservice.model.UserProfile;
import com.portal.userprofileservice.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<UserProfile> findByUserRole(UserRole userRole, Pageable pageable);

    Page<UserProfile> findByAccountStatus(AccountStatus accountStatus, Pageable pageable);

    Page<UserProfile> findByFaculty(String faculty, Pageable pageable);

    Page<UserProfile> findByIsActivelyLookingTrue(Pageable pageable);

    @Query("SELECT u FROM UserProfile u WHERE " +
           "(:query IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:role IS NULL OR u.userRole = :role) AND " +
           "(:status IS NULL OR u.accountStatus = :status)")
    Page<UserProfile> searchUsers(@Param("query") String query,
                                  @Param("role") UserRole role,
                                  @Param("status") AccountStatus status,
                                  Pageable pageable);

    @Query("SELECT DISTINCT s.userId FROM Skill s WHERE LOWER(s.skillName) IN :skills")
    List<String> findUserIdsBySkills(@Param("skills") List<String> skills);
}
