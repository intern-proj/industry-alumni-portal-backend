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

    @org.springframework.beans.factory.annotation.Value("${app.backend.url:${API_GATEWAY_URL:https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io}}")
    private String defaultBackendUrl;

    private String resolveBaseUrl(HttpServletRequest request) {
        if (request == null) {
            return defaultBackendUrl;
        }
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost != null && !forwardedHost.isBlank() && !forwardedHost.contains("event-participation-service") && !forwardedHost.contains("localhost")) {
            String proto = (forwardedProto != null && !forwardedProto.isBlank()) ? forwardedProto : "https";
            return proto + "://" + forwardedHost;
        }
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !origin.contains("localhost")) {
            return origin;
        }
        return defaultBackendUrl;
    }

    @PostMapping("/api/v1/events/{eventId}/qr-sessions")
    public ResponseEntity<QrSessionResponse> generate(@PathVariable UUID eventId,
                                                        @Valid @RequestBody QrSessionRequest request,
                                                        HttpServletRequest httpRequest) {
        QrSession created = qrSessionService.generate(eventId, request);
        String baseUrl = resolveBaseUrl(httpRequest);
        QrSessionResponse response = QrSessionResponse.from(created, baseUrl);
        return ResponseEntity.created(URI.create("/api/v1/qr-sessions/" + created.getQrId())).body(response);
    }

    @GetMapping("/api/v1/qr-sessions/{qrId}")
    public ResponseEntity<QrSessionResponse> getById(@PathVariable UUID qrId, HttpServletRequest httpRequest) {
        QrSession session = qrSessionService.getById(qrId);
        String baseUrl = resolveBaseUrl(httpRequest);
        return ResponseEntity.ok(QrSessionResponse.from(session, baseUrl));
    }

    @GetMapping("/api/v1/events/{eventId}/qr-sessions")
    public ResponseEntity<List<QrSessionResponse>> getByEvent(@PathVariable UUID eventId, HttpServletRequest httpRequest) {
        String baseUrl = resolveBaseUrl(httpRequest);
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