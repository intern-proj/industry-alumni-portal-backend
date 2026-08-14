package com.nsbm.application_service.dto;

import com.nsbm.application_service.model.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusChangeRequest {

    @NotNull
    private ApplicationStatus newStatus;

    @NotBlank
    private String changedBy;

    private String changeReason;
}
