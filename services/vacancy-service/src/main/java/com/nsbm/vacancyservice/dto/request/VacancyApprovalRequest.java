package com.nsbm.vacancyservice.dto.request;

import com.nsbm.vacancyservice.entity.VacancyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyApprovalRequest {

    @NotNull(message = "Approval status is required (APPROVED or REJECTED)")
    private VacancyStatus status;

    private String rejectionReason;

    private String comments;
}
