package com.portal.event_participation_service.repository;

import com.portal.event_participation_service.entity.QrSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrSessionRepository extends JpaRepository<QrSession, UUID> {
    List<QrSession> findByEventId(UUID eventId);
    Optional<QrSession> findByQrCodeValue(String qrCodeValue);
}
