package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.PendingStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingStaffRepository extends JpaRepository<PendingStaff, Long> {
    boolean existsByEmail(String email);
    Optional<PendingStaff> findByInvitationToken(String invitationToken);

}