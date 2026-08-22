package com.nsbm.application_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationRequest {

    @NotNull
    private UUID vacancyId;

    @NotNull
    private UUID alumniId;

    @NotBlank
    @Size(max = 500)
    private String resumeUrl;

    private String coverLetter;
}
