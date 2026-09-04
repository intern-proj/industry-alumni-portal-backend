package com.nsbm.eventmanagementservice.repository;
import com.nsbm.eventmanagementservice.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, Long> {
    List<Agenda> findByEventIdOrderBySequenceOrderAsc(Long eventId);
}
