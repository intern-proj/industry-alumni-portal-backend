package com.portal.platformservice.repository;

import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VacancyApproval;
import com.portal.platformservice.entity.VacancyApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacancyApprovalRepository extends JpaRepository<VacancyApproval, UUID> {

    Optional<VacancyApproval> findByVacancyId(UUID vacancyId);

    Page<VacancyApproval> findByStatus(VacancyApprovalStatus status, Pageable pageable);

    List<VacancyApproval> findBySyncStatus(SyncStatus syncStatus);
}
