package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.PendingPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingPartnerRepository extends JpaRepository<PendingPartner, Long> {
    Optional<PendingPartner> findByRegistrationToken(String registrationToken);
    boolean existsByEmail(String email);
}

