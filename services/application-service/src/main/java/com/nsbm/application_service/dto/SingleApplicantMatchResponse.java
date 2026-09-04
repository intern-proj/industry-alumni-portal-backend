package com.nsbm.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleApplicantMatchResponse {
    @JsonProperty("match_percentage")
    private Integer matchPercentage;

    @JsonProperty("matched_skills")
    private List<String> matchedSkills;

    @JsonProperty("missing_skills")
    private List<String> missingSkills;

    @JsonProperty("fit_summary")
    private String fitSummary;

    @JsonProperty("strong_fortes")
    private List<String> strongFortes;

    @JsonProperty("score_breakdown")
    private ScoreBreakdownDto scoreBreakdown;

    @JsonProperty("match_tier")
    private String matchTier;
}
