package com.nsbm.eventmanagementservice.repository;

import com.nsbm.eventmanagementservice.model.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {
    Optional<EventAttendance> findByAgendaIdAndStudentId(Long agendaId, Long studentId);
    long countByAgendaId(Long agendaId);
}
