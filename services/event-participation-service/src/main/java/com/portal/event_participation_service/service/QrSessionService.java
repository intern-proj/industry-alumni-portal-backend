package com.portal.event_participation_service.service;

import com.portal.event_participation_service.dto.QrSessionRequest;
import com.portal.event_participation_service.dto.QrVerifyResponse;
import com.portal.event_participation_service.entity.QrSession;
import com.portal.event_participation_service.exception.ResourceNotFoundException;
import com.portal.event_participation_service.repository.QrSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrSessionService {

    private final QrSessionRepository qrSessionRepository;

    public QrSession generate(UUID eventId, QrSessionRequest request) {
        Instant now = Instant.now();
        QrSession session = QrSession.builder()
                .eventId(eventId)
                .qrCodeValue(UUID.randomUUID().toString())
   
                .generatedAt(now)
                .expiresAt(now.plus(request.validForMinutes(), ChronoUnit.MINUTES))
                .active(true)
                .build();
        return qrSessionRepository.save(session);
    }

    public QrSession getById(UUID qrId) {
        return qrSessionRepository.findById(qrId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QR session with ID " + qrId + " was not found."));
    }

    public List<QrSession> getByEvent(UUID eventId) {
        return qrSessionRepository.findByEventId(eventId);
    }

    public QrVerifyResponse verify(String qrCodeValue) {
        QrSession session = qrSessionRepository.findByQrCodeValue(qrCodeValue)
                .orElseThrow(() -> new ResourceNotFoundException("QR code was not found."));
        boolean isActive = session.isActive() && Instant.now().isBefore(session.getExpiresAt());
        return new QrVerifyResponse(session.getEventId(), isActive, session.getExpiresAt());
    }

    public void deactivate(UUID qrId) {
        QrSession session = getById(qrId);
        session.setActive(false);
        qrSessionRepository.save(session);
    }
}