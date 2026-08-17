package com.portal.platformservice.repository;

import com.portal.platformservice.entity.PartnerVerification;
import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerVerificationRepository extends JpaRepository<PartnerVerification, UUID> {

    Optional<PartnerVerification> findByUserId(UUID userId);

    Page<PartnerVerification> findByStatus(VerificationStatus status, Pageable pageable);

    List<PartnerVerification> findBySyncStatus(SyncStatus syncStatus);
}
