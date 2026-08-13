package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.JobPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobPreferenceRepository extends JpaRepository<JobPreference, String> {
    List<JobPreference> findByUserId(String userId);
}