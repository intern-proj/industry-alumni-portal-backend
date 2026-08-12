package com.portal.certificateservice.repository;

import com.portal.certificateservice.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    
    Optional<Certificate> findByVerificationCode(String verificationCode);

    List<Certificate> findByStudentId(UUID studentId);

    List<Certificate> findByEventId(UUID eventId);

    boolean existsByStudentIdAndEventId(UUID studentId, UUID eventId);

    Optional<Certificate> findByStudentIdAndEventId(UUID studentId, UUID eventId);
    
    long countByStatus(String status);
}