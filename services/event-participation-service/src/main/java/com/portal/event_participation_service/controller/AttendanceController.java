package com.portal.event_participation_service.controller;

import com.portal.event_participation_service.dto.AttendanceResponse;
import com.portal.event_participation_service.dto.CheckinRequest;
import com.portal.event_participation_service.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkin")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody CheckinRequest request) {
        AttendanceResponse response = attendanceService.checkIn(request);
        return ResponseEntity
                .created(URI.create("/api/v1/attendance/" + response.attendanceId()))
                .body(response);
    }

    @GetMapping("/registration/{registrationId}")
    public ResponseEntity<AttendanceResponse> getByRegistration(@PathVariable UUID registrationId) {
        return ResponseEntity.ok(attendanceService.getByRegistration(registrationId));
    }
}