package com.portal.platformservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyApprovalSubmitRequest {

    @NotNull
    private String vacancyId;

    @NotNull
    private UUID companyUserId;

    private UUID submittedByUserId;

    @NotBlank
    private String vacancyTitleSnapshot;

    @NotBlank
    private String companyNameSnapshot;
}
