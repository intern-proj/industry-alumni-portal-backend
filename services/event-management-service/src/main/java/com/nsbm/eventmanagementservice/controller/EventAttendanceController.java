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
        
        // The QR code will point to our frontend route
        String qrUrl = origin + "/?session_token=" + token;

        return ResponseEntity.ok(Map.of("qrUrl", qrUrl, "token", token));
    }

    @PostMapping("/attendance/scan")
    public ResponseEntity<Map<String, String>> scanAttendance(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing token"));
        }

        String agendaIdStr = jwtTokenProvider.getUsernameFromToken(token);
        Long agendaId = Long.parseLong(agendaIdStr);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        // Assuming user ID is the principal name for logged in students
        String currentPrincipalName = auth.getName();
        Long studentId;
        try {
            studentId = Long.parseLong(currentPrincipalName);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid student ID format in token"));
        }

        boolean hasStudentRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        
        if (!hasStudentRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Only students can mark attendance"));
        }

        Optional<EventAttendance> existing = attendanceRepository.findByAgendaIdAndStudentId(agendaId, studentId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(Map.of("message", "Attendance already recorded for this session."));
        }

        EventAttendance attendance = EventAttendance.builder()
                .agendaId(agendaId)
                .studentId(studentId)
                .build();
        
        attendanceRepository.save(attendance);
        
        return ResponseEntity.ok(Map.of("message", "Attendance successfully recorded!"));
    }
}
