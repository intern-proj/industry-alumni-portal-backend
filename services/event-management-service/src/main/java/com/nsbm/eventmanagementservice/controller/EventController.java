package com.nsbm.eventmanagementservice.controller;

import com.nsbm.eventmanagementservice.dto.*;
import com.nsbm.eventmanagementservice.model.EventStatus;
import com.nsbm.eventmanagementservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long coordinatorUserId) {

        if (status != null) {
            return ResponseEntity.ok(eventService.getEventsByStatus(status));
        }
        if (venueId != null) {
            return ResponseEntity.ok(eventService.getEventsByVenue(venueId));
        }
        if (coordinatorUserId != null) {
            return ResponseEntity.ok(eventService.getEventsByCoordinator(coordinatorUserId));
        }
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/speaker/{speakerId}")
    public ResponseEntity<List<EventResponse>> getEventsBySpeaker(@PathVariable Long speakerId) {
        return ResponseEntity.ok(eventService.getEventsBySpeakerId(speakerId));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventStatusRequest request) {
        return ResponseEntity.ok(eventService.updateStatus(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<EventResponse> rescheduleEvent(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleEventRequest request) {
        return ResponseEntity.ok(eventService.rescheduleEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PostMapping("/{id}/coordinator")
    public ResponseEntity<EventResponse> assignCoordinator(
            @PathVariable Long id,
            @Valid @RequestBody AssignCoordinatorRequest request) {
        return ResponseEntity.ok(eventService.assignCoordinator(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @DeleteMapping("/{id}/coordinator")
    public ResponseEntity<EventResponse> removeCoordinator(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.removeCoordinator(id));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @PostMapping("/{id}/gallery")
    public ResponseEntity<EventResponse> addGalleryImages(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        List<String> images = new ArrayList<>();
        if (payload.containsKey("images") && payload.get("images") instanceof List<?>) {
            for (Object obj : (List<?>) payload.get("images")) {
                if (obj != null) images.add(obj.toString());
            }
        } else if (payload.containsKey("image") && payload.get("image") != null) {
            images.add(payload.get("image").toString());
        }
        return ResponseEntity.ok(eventService.addGalleryImages(id, images));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR', 'INTERNSHIP_COORDINATOR')")
    @DeleteMapping("/{id}/gallery")
    public ResponseEntity<EventResponse> removeGalleryImage(
            @PathVariable Long id,
            @RequestParam String imageUrl) {
        return ResponseEntity.ok(eventService.removeGalleryImage(id, imageUrl));
    }
}
