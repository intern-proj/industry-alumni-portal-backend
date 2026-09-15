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

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id, isStaffUser()));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) Long venueId,
            @RequestParam(required = false) Long coordinatorUserId,
            @RequestParam(required = false, defaultValue = "false") boolean includeDrafts) {

        boolean isStaff = isStaffUser();

        if (status != null) {
            if (status == EventStatus.DRAFT && !isStaff) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(eventService.getEventsByStatus(status));
        }
        if (venueId != null) {
            List<EventResponse> list = eventService.getEventsByVenue(venueId);
            if (!isStaff) {
                list = list.stream().filter(e -> e.getStatus() != EventStatus.DRAFT).toList();
            }
            return ResponseEntity.ok(list);
        }
        if (coordinatorUserId != null) {
            List<EventResponse> list = eventService.getEventsByCoordinator(coordinatorUserId);
            if (!isStaff) {
                list = list.stream().filter(e -> e.getStatus() != EventStatus.DRAFT).toList();
            }
            return ResponseEntity.ok(list);
        }

        boolean showDrafts = isStaff && includeDrafts;
        return ResponseEntity.ok(eventService.getAllEvents(showDrafts));
    }

    private boolean isStaffUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return role.equals("ROLE_SYSTEM_ADMIN") || role.equals("SYSTEM_ADMIN")
                    || role.equals("ROLE_EVENT_COORDINATOR") || role.equals("EVENT_COORDINATOR")
                    || role.equals("ROLE_ADMINISTRATIVE_STAFF") || role.equals("ADMINISTRATIVE_STAFF")
                    || role.equals("ROLE_FACULTY_MANAGEMENT") || role.equals("FACULTY_MANAGEMENT")
                    || role.equals("ROLE_FACULTY_COORDINATOR") || role.equals("FACULTY_COORDINATOR");
        });
    }

    @GetMapping("/speaker/{speakerId}")
    public ResponseEntity<List<EventResponse>> getEventsBySpeaker(@PathVariable Long speakerId) {
        return ResponseEntity.ok(eventService.getEventsBySpeakerId(speakerId));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventStatusRequest request) {
        return ResponseEntity.ok(eventService.updateStatus(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<EventResponse> rescheduleEvent(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleEventRequest request) {
        return ResponseEntity.ok(eventService.rescheduleEvent(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @PostMapping("/{id}/coordinator")
    public ResponseEntity<EventResponse> assignCoordinator(
            @PathVariable Long id,
            @Valid @RequestBody AssignCoordinatorRequest request) {
        return ResponseEntity.ok(eventService.assignCoordinator(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF', 'FACULTY_MANAGEMENT', 'FACULTY_COORDINATOR')")
    @DeleteMapping("/{id}/coordinator")
    public ResponseEntity<EventResponse> removeCoordinator(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.removeCoordinator(id));
    }
}
