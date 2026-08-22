package com.nsbm.application_service.dto;

import com.nsbm.application_service.model.StageStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentStageResponse {
    private UUID id;
    private UUID applicationId;
    private String stageName;
    private StageStatus stageStatus;
    private LocalDateTime scheduledAt;
    private Double score;
    private String feedback;
    private String interviewerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
