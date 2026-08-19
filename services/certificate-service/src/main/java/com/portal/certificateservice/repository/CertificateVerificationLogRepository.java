package com.portal.certificateservice.repository;

import com.portal.certificateservice.entity.CertificateVerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface CertificateVerificationLogRepository extends JpaRepository<CertificateVerificationLog, UUID> {

    List<CertificateVerificationLog> findByCertificateIdOrderByVerifiedAtDesc(UUID certificateId);

    long countByCertificateId(UUID certificateId);
}
