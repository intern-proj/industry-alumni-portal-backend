package com.portal.event_participation_service.controller;

import com.portal.event_participation_service.dto.QrSessionRequest;
import com.portal.event_participation_service.dto.QrSessionResponse;
import com.portal.event_participation_service.dto.QrVerifyResponse;
import com.portal.event_participation_service.entity.QrSession;
import com.portal.event_participation_service.service.QrSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class QrSessionController {

    private final QrSessionService qrSessionService;

    @PostMapping("/api/v1/events/{eventId}/qr-sessions")
    public ResponseEntity<QrSessionResponse> generate(@PathVariable UUID eventId,
                                                        @Valid @RequestBody QrSessionRequest request,
                                                        HttpServletRequest httpRequest) {
        QrSession created = qrSessionService.generate(eventId, request);
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        QrSessionResponse response = QrSessionResponse.from(created, baseUrl);
        return ResponseEntity.created(URI.create("/api/v1/qr-sessions/" + created.getQrId())).body(response);
    }

    @GetMapping("/api/v1/qr-sessions/{qrId}")
    public ResponseEntity<QrSessionResponse> getById(@PathVariable UUID qrId, HttpServletRequest httpRequest) {
        QrSession session = qrSessionService.getById(qrId);
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        return ResponseEntity.ok(QrSessionResponse.from(session, baseUrl));
    }

    @GetMapping("/api/v1/events/{eventId}/qr-sessions")
    public ResponseEntity<List<QrSessionResponse>> getByEvent(@PathVariable UUID eventId, HttpServletRequest httpRequest) {
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        List<QrSessionResponse> responses = qrSessionService.getByEvent(eventId).stream()
                .map(s -> QrSessionResponse.from(s, baseUrl))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/v1/qr-sessions/{qrCodeValue}/verify")
    public ResponseEntity<QrVerifyResponse> verify(@PathVariable String qrCodeValue) {
        return ResponseEntity.ok(qrSessionService.verify(qrCodeValue));
    }

    @DeleteMapping("/api/v1/qr-sessions/{qrId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID qrId) {
        qrSessionService.deactivate(qrId);
        return ResponseEntity.noContent().build();
    }
}