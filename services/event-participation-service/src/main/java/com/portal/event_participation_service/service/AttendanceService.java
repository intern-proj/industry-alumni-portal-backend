package com.portal.event_participation_service.service;

import com.portal.event_participation_service.dto.AttendanceResponse;
import com.portal.event_participation_service.dto.CheckinRequest;
import com.portal.event_participation_service.entity.Attendance;
import com.portal.event_participation_service.entity.QrSession;
import com.portal.event_participation_service.entity.Registration;
import com.portal.event_participation_service.exception.ResourceNotFoundException;
import com.portal.event_participation_service.repository.AttendanceRepository;
import com.portal.event_participation_service.repository.QrSessionRepository;
import com.portal.event_participation_service.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final QrSessionRepository qrSessionRepository;

    public AttendanceResponse checkIn(CheckinRequest request) {
        // 1. Confirm the registration actually exists
        Registration registration = registrationRepository.findById(request.registrationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registration with ID " + request.registrationId() + " was not found."));

        // 2. Confirm the QR session exists and is still active
        QrSession qrSession = qrSessionRepository.findByQrCodeValue(request.qrCodeValue())
                .orElseThrow(() -> new ResourceNotFoundException("QR code was not found."));

        boolean qrActive = qrSession.isActive() && Instant.now().isBefore(qrSession.getExpiresAt());
        if (!qrActive) {
            throw new IllegalStateException("This QR code has expired or is no longer active.");
        }

        // 3. Confirm the QR session actually belongs to the same event as the registration
        if (!qrSession.getEventId().equals(registration.getEventId())) {
            throw new IllegalStateException("This QR code does not match the registered event.");
        }

        // 4. Prevent duplicate check-in
        if (attendanceRepository.existsByRegistrationId(request.registrationId())) {
            throw new IllegalStateException(
                    "Registration " + request.registrationId() + " has already checked in.");
        }

        // 5. Record attendance
        Attendance attendance = Attendance.builder()
                .registrationId(request.registrationId())
                .qrId(qrSession.getQrId())
                .checkinTime(Instant.now())
                .checkinMethod(Attendance.CheckinMethod.QR_SCAN)
                .status(Attendance.AttendanceStatus.PRESENT)
                .build();

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    public AttendanceResponse getByRegistration(UUID registrationId) {
        Attendance attendance = attendanceRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No attendance record found for registration " + registrationId));
        return AttendanceResponse.from(attendance);
    }
}