package com.nsbm.eventmanagementservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate_eligibility_criteria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateEligibilityCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Column(name = "min_attendance_percentage")
    private Integer minAttendancePercentage;

    @Column(name = "requires_feedback_submission", nullable = false)
    private boolean requiresFeedbackSubmission;

    @Column(name = "min_sessions_attended")
    private Integer minSessionsAttended;

    @Column(name = "other_criteria_notes", columnDefinition = "TEXT")
    private String otherCriteriaNotes;

    @Column(name = "template_image")
    private String templateImage;

    @Column(name = "name_pos_x")
    private Integer namePosX;

    @Column(name = "name_pos_y")
    private Integer namePosY;

    @Column(name = "name_font_size")
    private Integer nameFontSize;

    @Column(name = "name_font_color")
    private String nameFontColor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
