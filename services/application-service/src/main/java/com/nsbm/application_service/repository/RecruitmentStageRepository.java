package com.nsbm.application_service.repository;

import com.nsbm.application_service.model.RecruitmentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecruitmentStageRepository extends JpaRepository<RecruitmentStage, UUID> {
    List<RecruitmentStage> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
