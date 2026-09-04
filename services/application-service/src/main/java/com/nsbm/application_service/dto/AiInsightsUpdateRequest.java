package com.nsbm.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightsUpdateRequest {
    private Integer matchPercentage;
    private String matchedSkills;
    private String missingSkills;
    private String fitSummary;
    private String strongFortes;
    private String scoreBreakdown;
}
