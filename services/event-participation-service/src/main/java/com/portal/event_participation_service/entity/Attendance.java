package com.portal.event_participation_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attendance_id", updatable = false, nullable = false)
    private UUID attendanceId;

    @Column(name = "registration_id", nullable = false, unique = true)
    private UUID registrationId;

    @Column(name = "qr_id", nullable = false)
    private UUID qrId;

    @Column(name = "checkin_time", nullable = false, updatable = false)
    private Instant checkinTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_method", nullable = false, length = 20)
    private CheckinMethod checkinMethod;

    @Column(name = "checked_in_by")
    private UUID checkedInBy; // external ref, nullable — set only for manual staff override

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    public enum CheckinMethod {
        QR_SCAN, MANUAL_OVERRIDE
    }

    public enum AttendanceStatus {
        PRESENT, ABSENT
    }
}