package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.IndustryPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndustryPartnerRepository extends JpaRepository<IndustryPartner, Long> {
    Optional<IndustryPartner> findByUsername(String username);

    static boolean existsByUsername(String username) {
        return false;
    }

    static boolean existsByEmail(String email) {
        return false;
    }
}

