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
    private Long vacancyId;

    @NotNull
    private String alumniId;

    @NotBlank
    @Size(max = 500)
    private String resumeUrl;

    private String coverLetter;

    private String studentName;
    private String studentEmail;
    private String program;
    private String gpa;
    private String profilePicUrl;

    private String vacancyTitle;
    private String vacancyRequirements;
    private String vacancyDescription;
    private String vacancyTags;
}
