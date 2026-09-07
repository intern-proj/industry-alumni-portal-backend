package com.nsbm.eventmanagementservice.controller;

import com.nsbm.common.security.JwtTokenProvider;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;
import com.nsbm.eventmanagementservice.service.GuestSpeakerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.nsbm.notification_service.dto.UpdateEmailDTO;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/guest-speakers")
@RequiredArgsConstructor
@Slf4j
public class GuestSpeakerController {
    private final GuestSpeakerService guestSpeakerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping
    public ResponseEntity<GuestSpeakerResponse> createSpeaker(@Valid @RequestBody GuestSpeakerRequest request) {
        GuestSpeakerResponse response = guestSpeakerService.createSpeaker(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<GuestSpeakerResponse> getMe() {
        String currentPrincipalName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Long speakerId = Long.parseLong(currentPrincipalName);
        return ResponseEntity.ok(guestSpeakerService.getSpeakerById(speakerId));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping("/{id}/invite")
    public ResponseEntity<Void> sendInvite(@PathVariable Long id, HttpServletRequest httpRequest) {
        GuestSpeakerResponse speaker = guestSpeakerService.getSpeakerById(id);
        String token = jwtTokenProvider.generateToken(speaker.getId().toString(), speaker.getEmail(), "GUEST_SPEAKER", "GUEST");
        
        String origin = httpRequest.getHeader("Origin");
        if (origin == null || origin.isEmpty()) {
            origin = frontendUrl; // fallback
        }
        String inviteLink = origin + "/?token=" + token;
        
        UpdateEmailDTO message = new UpdateEmailDTO(
                speaker.getEmail(),
                speaker.getFullName(),
                "GENERAL_UPDATE",
                "You have been invited as a Guest Speaker. Please click the link to verify your identity and access your dashboard: " + inviteLink,
                inviteLink
        );

        rabbitTemplate.convertAndSend("notification.exchange", "notification.update", message);
        log.info("Published guest speaker invitation to RabbitMQ for {}", speaker.getEmail());
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestSpeakerResponse> getSpeakerById(@PathVariable Long id) {
        return ResponseEntity.ok(guestSpeakerService.getSpeakerById(id));
    }

    @GetMapping
    public ResponseEntity<List<GuestSpeakerResponse>> getAllSpeakers(
            @RequestParam(required = false) Long organizationId) {

        if (organizationId != null) {
            return ResponseEntity.ok(guestSpeakerService.getSpeakersByOrganization(organizationId));
        }
        return ResponseEntity.ok(guestSpeakerService.getAllSpeakers());
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<GuestSpeakerResponse> updateSpeaker(
            @PathVariable Long id,
            @Valid @RequestBody GuestSpeakerRequest request) {
        return ResponseEntity.ok(guestSpeakerService.updateSpeaker(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpeaker(@PathVariable Long id) {
        guestSpeakerService.deleteSpeaker(id);
        return ResponseEntity.noContent().build();
    }
}
