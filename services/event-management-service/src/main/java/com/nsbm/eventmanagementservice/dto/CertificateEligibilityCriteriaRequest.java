package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateEligibilityCriteriaRequest {
    @Min(value = 0, message = "Minimum attendance percentage cannot be negative")
    @Max(value = 100, message = "Minimum attendance percentage cannot exceed 100")
    private Integer minAttendancePercentage;

    private boolean requiresFeedbackSubmission;

    @Min(value = 0, message = "Minimum sessions attended cannot be negative")
    private Integer minSessionsAttended;

    private String otherCriteriaNotes;

}
