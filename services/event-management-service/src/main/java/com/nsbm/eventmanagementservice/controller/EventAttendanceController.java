package com.nsbm.eventmanagementservice.controller;

import com.nsbm.common.security.JwtTokenProvider;
import com.nsbm.eventmanagementservice.model.EventAttendance;
import com.nsbm.eventmanagementservice.repository.EventAttendanceRepository;
import com.nsbm.eventmanagementservice.repository.AgendaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventAttendanceController {
    private final EventAttendanceRepository attendanceRepository;
    private final AgendaRepository agendaRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.url:${FRONTEND_URL:https://wonderful-wave-0320abf00.3.azurestaticapps.net}}")
    private String frontendUrl;

    @GetMapping("/agendas/{agendaId}/qr-token")
    public ResponseEntity<Map<String, String>> generateQrToken(@PathVariable Long agendaId, HttpServletRequest httpRequest) {
        if (!agendaRepository.existsById(agendaId)) {
            return ResponseEntity.notFound().build();
        }

        // Generate a JWT token containing the agenda ID. 
        // We'll use role "SESSION_ATTENDANCE" to distinguish it if needed.
        String token = jwtTokenProvider.generateToken(agendaId.toString(), "session-attendance@nsbm.lk", "SESSION_ATTENDANCE", "SYSTEM");

        String origin = httpRequest.getHeader("Origin");
        if (origin == null || origin.isBlank() || origin.contains("localhost")) {
            String referer = httpRequest.getHeader("Referer");
            if (referer != null && !referer.isBlank() && !referer.contains("localhost")) {
                try {
                    java.net.URI uri = java.net.URI.create(referer);
                    origin = uri.getScheme() + "://" + uri.getAuthority();
                } catch (Exception ignored) {
                    origin = frontendUrl;
                }
            } else {
                origin = frontendUrl;
            }
        }
        
        // The QR code will point to our dedicated frontend attendance route
        String qrUrl = origin + "/attendance/mark?session_token=" + token;

        return ResponseEntity.ok(Map.of("qrUrl", qrUrl, "token", token));
    }

    @PostMapping("/attendance/scan")
    public ResponseEntity<Map<String, Object>> scanAttendance(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired check-in token. Please scan the latest QR code."));
        }

        String agendaIdStr = jwtTokenProvider.getUsernameFromToken(token);
        Long agendaId = Long.parseLong(agendaIdStr);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required. Please log in as a student to record attendance."));
        }

        boolean hasStudentRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));
        
        if (!hasStudentRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Only enrolled NSBM students can scan and mark attendance for event sessions."));
        }

        // Robust student ID resolution
        Long studentId = null;
        if (request.containsKey("studentId") && request.get("studentId") != null && !request.get("studentId").isBlank()) {
            try {
                studentId = Long.parseLong(String.valueOf(request.get("studentId")));
            } catch (Exception ignored) {}
        }
        if (studentId == null) {
            try {
                studentId = Long.parseLong(auth.getName());
            } catch (Exception ignored) {}
        }
        if (studentId == null) {
            studentId = (long) (Math.abs(auth.getName().hashCode()) % 1000000 + 1);
        }

        var agendaOpt = agendaRepository.findById(agendaId);
        if (agendaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Event session not found."));
        }
        var agenda = agendaOpt.get();
        var event = agenda.getEvent();

        Optional<EventAttendance> existing = attendanceRepository.findByAgendaIdAndStudentId(agendaId, studentId);
        boolean alreadyRecorded = existing.isPresent();

        if (!alreadyRecorded) {
            EventAttendance attendance = EventAttendance.builder()
                    .agendaId(agendaId)
                    .studentId(studentId)
                    .scannedAt(java.time.LocalDateTime.now())
                    .build();
            attendanceRepository.save(attendance);
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", alreadyRecorded ? "ALREADY_RECORDED" : "RECORDED");
        result.put("message", alreadyRecorded ? "Attendance already recorded for this session." : "Attendance successfully marked!");
        result.put("eventId", event != null ? String.valueOf(event.getId()) : "");
        result.put("eventTitle", event != null ? event.getTitle() : "University Event");
        result.put("sessionTitle", agenda.getTitle() != null ? agenda.getTitle() : "Session Check-In");
        result.put("startTime", agenda.getStartTime() != null ? agenda.getStartTime().toString() : (event != null && event.getStartDateTime() != null ? event.getStartDateTime().toString() : ""));
        result.put("venue", agenda.getVenue() != null ? agenda.getVenue().getName() : (event != null && event.getVenue() != null ? event.getVenue().getName() : "NSBM Green University"));
        result.put("certificateEligible", true);
        result.put("studentUsername", auth.getName());
        result.put("scannedAt", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(result);
    }
}
