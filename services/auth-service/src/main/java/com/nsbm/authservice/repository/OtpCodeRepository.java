package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findTopBySessionTokenAndCodeOrderByCreatedAtDesc(String sessionToken, String code);
    Optional<OtpCode> findTopByUsernameAndCodeOrderByCreatedAtDesc(String username, String code);
    Optional<OtpCode> findTopBySessionTokenOrderByCreatedAtDesc(String sessionToken);
    void deleteByExpiresAtBefore(LocalDateTime now);
    void deleteByUsername(String username);
}