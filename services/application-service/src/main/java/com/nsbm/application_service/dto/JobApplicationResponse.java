package com.nsbm.application_service.dto;

import com.nsbm.application_service.model.ApplicationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationResponse {
    private UUID id;
    private Long vacancyId;
    private UUID alumniId;
    private String resumeUrl;
    private String coverLetter;
    private String studentName;
    private String studentEmail;
    private String program;
    private String gpa;
    private String profilePicUrl;
    private Integer matchPercentage;
    private String matchedSkills;
    private String missingSkills;
    private String fitSummary;
    private String strongFortes;
    private String scoreBreakdown;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
