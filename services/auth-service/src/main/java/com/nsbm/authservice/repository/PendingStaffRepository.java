package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.PendingStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PendingStaffRepository extends JpaRepository<PendingStaff, Long> {
    boolean existsByEmail(String email);
    Optional<PendingStaff> findByEmail(String email);
    Optional<PendingStaff> findByInvitationToken(String invitationToken);

    @Modifying
    @Transactional
    @Query("DELETE FROM PendingStaff p WHERE p.email = :email")
    void deleteByEmail(String email);
}