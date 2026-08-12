package com.nsbm.eventmanagementservice.repository;
import com.nsbm.eventmanagementservice.model.GuestSpeaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestSpeakerRepository extends JpaRepository<GuestSpeaker, Long>{
    List<GuestSpeaker> findByOrganizationId(Long organizationId);
}
