package com.nsbm.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDto {
    @JsonProperty("skills_coverage")
    private Integer skillsCoverage;

    @JsonProperty("semantic_alignment")
    private Integer semanticAlignment;

    @JsonProperty("cross_encoder_score")
    private Integer crossEncoderScore;

    @JsonProperty("institutional_fit")
    private Integer institutionalFit;
}
