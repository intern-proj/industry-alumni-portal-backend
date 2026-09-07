package com.portal.user_service.repository;

import com.portal.user_service.model.SpeakerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpeakerProfileRepository extends JpaRepository<SpeakerProfile, String> {
    Optional<SpeakerProfile> findByUserId(String userId);

    @Query("SELECT s FROM SpeakerProfile s WHERE " +
           "(:query IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.organization) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.expertiseTags) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SpeakerProfile> searchSpeakers(@Param("query") String query, Pageable pageable);
}
