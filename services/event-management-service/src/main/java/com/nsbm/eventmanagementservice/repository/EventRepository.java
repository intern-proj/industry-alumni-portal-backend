package com.nsbm.eventmanagementservice.repository;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatus(EventStatus status);

    List<Event> findByVenueId(Long venueId);

    List<Event> findByCoordinatorUserId(Long coordinatorUserId);

    List<Event> findByOrganizationId(Long organizationId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT e FROM Event e JOIN e.agendas a JOIN a.lectures l WHERE l.speaker.id = :speakerId")
    List<Event> findDistinctByAgendasSpeakerId(@org.springframework.data.repository.query.Param("speakerId") Long speakerId);
}
