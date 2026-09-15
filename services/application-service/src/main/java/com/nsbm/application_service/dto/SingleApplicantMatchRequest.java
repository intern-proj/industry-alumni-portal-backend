package com.nsbm.application_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleApplicantMatchRequest {
    @JsonProperty("resume_url")
    private String resumeUrl;

    @JsonProperty("resume_text")
    private String resumeText;

    @JsonProperty("cover_letter")
    private String coverLetter;

    @JsonProperty("candidate_skills")
    private List<String> candidateSkills;

    @JsonProperty("vacancy_id")
    private Long vacancyId;

    @JsonProperty("vacancy_title")
    private String vacancyTitle;

    @JsonProperty("vacancy_description")
    private String vacancyDescription;

    @JsonProperty("vacancy_requirements")
    private String vacancyRequirements;

    @JsonProperty("vacancy_tags")
    private String vacancyTags;

    @JsonProperty("candidate_name")
    private String candidateName;

    @JsonProperty("candidate_email")
    private String candidateEmail;

    @JsonProperty("candidate_faculty")
    private String candidateFaculty;
}
