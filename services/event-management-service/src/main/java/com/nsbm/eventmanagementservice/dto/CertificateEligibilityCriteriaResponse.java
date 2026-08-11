package com.nsbm.eventmanagementservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateEligibilityCriteriaResponse {
    private Long id;
    private Long eventId;
    private Integer minAttendancePercentage;
    private boolean requiresFeedbackSubmission;
    private Integer minSessionsAttended;
    private String otherCriteriaNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
