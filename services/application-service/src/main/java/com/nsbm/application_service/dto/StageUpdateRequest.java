package com.nsbm.application_service.dto;

import com.nsbm.application_service.model.StageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageUpdateRequest {

    @NotNull
    private StageStatus stageStatus;

    private Double score;

    private String feedback;
}
