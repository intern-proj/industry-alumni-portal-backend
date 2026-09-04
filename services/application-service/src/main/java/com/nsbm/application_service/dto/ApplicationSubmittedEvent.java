package com.nsbm.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSubmittedEvent {
    private String applicationId;
    private Long vacancyId;
    private String alumniId;
    private String resumeUrl;
    private String coverLetter;
    private String vacancyTitle;
    private String vacancyRequirements;
    private String vacancyDescription;
    private String vacancyTags;
    private String candidateName;
    private String candidateEmail;
    private String candidateFaculty;
    private List<String> candidateSkills;
}
