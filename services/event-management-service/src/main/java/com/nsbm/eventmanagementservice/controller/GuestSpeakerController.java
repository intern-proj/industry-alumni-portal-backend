package com.nsbm.eventmanagementservice.controller;

import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;
import com.nsbm.eventmanagementservice.service.GuestSpeakerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guest-speakers")
@RequiredArgsConstructor
public class GuestSpeakerController {
    private final GuestSpeakerService guestSpeakerService;

    @PostMapping
    public ResponseEntity<GuestSpeakerResponse> createSpeaker(@Valid @RequestBody GuestSpeakerRequest request) {
        GuestSpeakerResponse response = guestSpeakerService.createSpeaker(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @PutMapping("/{id}")
    public ResponseEntity<GuestSpeakerResponse> updateSpeaker(
            @PathVariable Long id,
            @Valid @RequestBody GuestSpeakerRequest request) {
        return ResponseEntity.ok(guestSpeakerService.updateSpeaker(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpeaker(@PathVariable Long id) {
        guestSpeakerService.deleteSpeaker(id);
        return ResponseEntity.noContent().build();
    }
}
