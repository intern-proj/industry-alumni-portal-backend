package com.portal.user_service.repository;

import com.portal.user_service.model.JobPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobPreferenceRepository extends JpaRepository<JobPreference, String> {
    Optional<JobPreference> findByUserId(String userId);
    void deleteByUserId(String userId);
}