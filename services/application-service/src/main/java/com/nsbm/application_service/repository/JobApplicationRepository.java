package com.nsbm.application_service.repository;

import com.nsbm.application_service.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByVacancyId(Long vacancyId);
    List<JobApplication> findByAlumniId(UUID alumniId);
}
