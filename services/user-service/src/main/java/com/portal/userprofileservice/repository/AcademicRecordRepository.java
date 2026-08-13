package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.AcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, String> {
    Optional<AcademicRecord> findByUserId(String userId);
}
