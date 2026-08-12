package com.nsbm.application_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentStageRequest {

    @NotBlank
    private String stageName;

    private LocalDateTime scheduledAt;

    private String interviewerName;
}
