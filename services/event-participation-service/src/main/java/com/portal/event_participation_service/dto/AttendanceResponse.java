package com.portal.event_participation_service.dto;

import com.portal.event_participation_service.entity.Attendance;

import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(
        UUID attendanceId,
        UUID registrationId,
        UUID qrId,
        Instant checkinTime,
        String checkinMethod,
        String status
) {
    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getAttendanceId(), a.getRegistrationId(), a.getQrId(),
                a.getCheckinTime(), a.getCheckinMethod().name(), a.getStatus().name()
        );
    }
}