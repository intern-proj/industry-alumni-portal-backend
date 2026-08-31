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
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventStatusRequest request) {
        return ResponseEntity.ok(eventService.updateStatus(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<EventResponse> rescheduleEvent(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleEventRequest request) {
        return ResponseEntity.ok(eventService.rescheduleEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping("/{id}/coordinator")
    public ResponseEntity<EventResponse> assignCoordinator(
            @PathVariable Long id,
            @Valid @RequestBody AssignCoordinatorRequest request) {
        return ResponseEntity.ok(eventService.assignCoordinator(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @DeleteMapping("/{id}/coordinator")
    public ResponseEntity<EventResponse> removeCoordinator(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.removeCoordinator(id));
    }
}
