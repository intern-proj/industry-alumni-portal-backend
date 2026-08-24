package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {
    List<Resume> findByUserId(String userId);

    Optional<Resume> findByUserIdAndIsPrimaryTrue(String userId);

    @Modifying
    @Query("UPDATE Resume r SET r.isPrimary = false WHERE r.userId = :userId")
    void resetPrimaryResumes(@Param("userId") String userId);
}