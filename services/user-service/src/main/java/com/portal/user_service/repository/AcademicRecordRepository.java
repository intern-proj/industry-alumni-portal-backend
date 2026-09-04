package com.portal.user_service.repository;

import com.portal.user_service.model.AcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, String> {
    Optional<AcademicRecord> findByUserId(String userId);
    void deleteByUserId(String userId);
}
