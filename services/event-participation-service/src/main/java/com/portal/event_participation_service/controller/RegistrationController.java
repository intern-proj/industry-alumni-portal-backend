package com.portal.event_participation_service.controller;



import com.portal.event_participation_service.dto.RegistrationRequest;
import com.portal.event_participation_service.dto.RegistrationResponse;
import com.portal.event_participation_service.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> create(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse created = registrationService.register(request);
        return ResponseEntity
                .created(URI.create("/api/v1/registrations/" + created.registrationId()))
                .body(created);
    }

    @GetMapping("/{registrationId}")
    public ResponseEntity<RegistrationResponse> getById(@PathVariable UUID registrationId) {
        return ResponseEntity.ok(registrationService.getById(registrationId));
    }

    @GetMapping
    public ResponseEntity<List<RegistrationResponse>> getByEvent(@RequestParam UUID eventId) {
        return ResponseEntity.ok(registrationService.getByEvent(eventId));
    }
}