package com.nsbm.eventmanagementservice.controller;

import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;
import com.nsbm.eventmanagementservice.service.AgendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/agendas")
@RequiredArgsConstructor
public class AgendaController {
    private final AgendaService agendaService;

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PostMapping
    public ResponseEntity<AgendaResponse> createAgendaItem(@Valid @RequestBody AgendaRequest request) {
        AgendaResponse response = agendaService.createAgendaItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendaResponse> getAgendaItemById(@PathVariable Long id) {
        return ResponseEntity.ok(agendaService.getAgendaItemById(id));
    }

    @GetMapping
    public ResponseEntity<List<AgendaResponse>> getAgenda(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long speakerId) {

        if (eventId != null) {
            return ResponseEntity.ok(agendaService.getAgendaByEventId(eventId));
        }
        if (speakerId != null) {
            return ResponseEntity.ok(agendaService.getAgendaBySpeakerId(speakerId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<AgendaResponse> updateAgendaItem(
            @PathVariable Long id,
            @Valid @RequestBody AgendaRequest request) {
        return ResponseEntity.ok(agendaService.updateAgendaItem(id, request));
    }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'EVENT_COORDINATOR', 'ADMINISTRATIVE_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgendaItem(@PathVariable Long id) {
        agendaService.deleteAgendaItem(id);
        return ResponseEntity.noContent().build();
    }
}
