package com.nsbm.eventmanagementservice.repository;
import com.nsbm.eventmanagementservice.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long>{
}
