package com.nsbm.application_service.service;

import com.nsbm.application_service.dto.*;
import java.util.List;
import java.util.UUID;

public interface JobApplicationService {
    JobApplicationResponse createApplication(JobApplicationRequest request);
    JobApplicationResponse getApplicationById(UUID id);
    List<JobApplicationResponse> getApplicationsByVacancyId(Long vacancyId);
    List<JobApplicationResponse> getApplicationsByAlumniId(UUID alumniId);
    JobApplicationResponse updateApplicationStatus(UUID id, StatusChangeRequest request);
    List<StatusAuditResponse> getStatusAudits(UUID id);
    RecruitmentStageResponse scheduleStage(UUID id, RecruitmentStageRequest request);
    RecruitmentStageResponse updateStage(UUID id, UUID stageId, StageUpdateRequest request);
    List<RecruitmentStageResponse> getStages(UUID id);
}
