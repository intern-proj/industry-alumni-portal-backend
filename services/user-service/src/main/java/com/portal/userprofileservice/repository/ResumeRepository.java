package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {
    List<Resume> findByUserId(String userId);
}