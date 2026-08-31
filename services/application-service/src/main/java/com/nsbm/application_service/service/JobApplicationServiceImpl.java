package com.nsbm.application_service.service;

import com.nsbm.application_service.dto.*;
import com.nsbm.application_service.exception.ResourceNotFoundException;
import com.nsbm.application_service.model.*;
import com.nsbm.application_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationStatusAuditRepository auditRepository;
    private final RecruitmentStageRepository recruitmentStageRepository;

    @Override
    @Transactional
    public JobApplicationResponse createApplication(JobApplicationRequest request) {
        JobApplication application = JobApplication.builder()
                .vacancyId(request.getVacancyId())
                .alumniId(request.getAlumniId())
                .resumeUrl(request.getResumeUrl())
                .coverLetter(request.getCoverLetter())
                .status(ApplicationStatus.PENDING)
                .build();

        JobApplication saved = jobApplicationRepository.save(application);

        ApplicationStatusAudit audit = ApplicationStatusAudit.builder()
                .applicationId(saved.getId())
                .previousStatus(null)
                .newStatus(ApplicationStatus.PENDING)
                .changedBy("SYSTEM")
                .changeReason("Initial application intake")
                .build();

        auditRepository.save(audit);

        return mapToResponse(saved);
    }

    @Override
    public JobApplicationResponse getApplicationById(UUID id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with id: " + id));
        return mapToResponse(application);
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByVacancyId(Long vacancyId) {
        return jobApplicationRepository.findByVacancyId(vacancyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobApplicationResponse> getApplicationsByAlumniId(UUID alumniId) {
        return jobApplicationRepository.findByAlumniId(alumniId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobApplicationResponse updateApplicationStatus(UUID id, StatusChangeRequest request) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with id: " + id));

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(request.getNewStatus());
        JobApplication updated = jobApplicationRepository.save(application);

        ApplicationStatusAudit audit = ApplicationStatusAudit.builder()
                .applicationId(updated.getId())
                .previousStatus(oldStatus)
                .newStatus(request.getNewStatus())
                .changedBy(request.getChangedBy())
                .changeReason(request.getChangeReason())
                .build();

        auditRepository.save(audit);

        return mapToResponse(updated);
    }

    @Override
    public List<StatusAuditResponse> getStatusAudits(UUID id) {
        if (!jobApplicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job application not found with id: " + id);
        }
        return auditRepository.findByApplicationIdOrderByChangedAtDesc(id).stream()
                .map(this::mapToAuditResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RecruitmentStageResponse scheduleStage(UUID id, RecruitmentStageRequest request) {
        if (!jobApplicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job application not found with id: " + id);
        }

        RecruitmentStage stage = RecruitmentStage.builder()
                .applicationId(id)
                .stageName(request.getStageName())
                .stageStatus(StageStatus.SCHEDULED)
                .scheduledAt(request.getScheduledAt())
                .interviewerName(request.getInterviewerName())
                .build();

        RecruitmentStage saved = recruitmentStageRepository.save(stage);
        return mapToStageResponse(saved);
    }

    @Override
    @Transactional
    public RecruitmentStageResponse updateStage(UUID id, UUID stageId, StageUpdateRequest request) {
        if (!jobApplicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job application not found with id: " + id);
        }

        RecruitmentStage stage = recruitmentStageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("Recruitment stage not found with id: " + stageId));

        if (!stage.getApplicationId().equals(id)) {
            throw new IllegalArgumentException("Stage does not belong to the specified application");
        }

        stage.setStageStatus(request.getStageStatus());
        stage.setScore(request.getScore());
        stage.setFeedback(request.getFeedback());

        RecruitmentStage updated = recruitmentStageRepository.save(stage);
        return mapToStageResponse(updated);
    }

    @Override
    public List<RecruitmentStageResponse> getStages(UUID id) {
        if (!jobApplicationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job application not found with id: " + id);
        }
        return recruitmentStageRepository.findByApplicationIdOrderByCreatedAtAsc(id).stream()
                .map(this::mapToStageResponse)
                .collect(Collectors.toList());
    }

    private JobApplicationResponse mapToResponse(JobApplication application) {
        return JobApplicationResponse.builder()
                .id(application.getId())
                .vacancyId(application.getVacancyId())
                .alumniId(application.getAlumniId())
                .resumeUrl(application.getResumeUrl())
                .coverLetter(application.getCoverLetter())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    private StatusAuditResponse mapToAuditResponse(ApplicationStatusAudit audit) {
        return StatusAuditResponse.builder()
                .id(audit.getId())
                .applicationId(audit.getApplicationId())
                .previousStatus(audit.getPreviousStatus())
                .newStatus(audit.getNewStatus())
                .changedBy(audit.getChangedBy())
                .changeReason(audit.getChangeReason())
                .changedAt(audit.getChangedAt())
                .build();
    }

    private RecruitmentStageResponse mapToStageResponse(RecruitmentStage stage) {
        return RecruitmentStageResponse.builder()
                .id(stage.getId())
                .applicationId(stage.getApplicationId())
                .stageName(stage.getStageName())
                .stageStatus(stage.getStageStatus())
                .scheduledAt(stage.getScheduledAt())
                .score(stage.getScore())
                .feedback(stage.getFeedback())
                .interviewerName(stage.getInterviewerName())
                .createdAt(stage.getCreatedAt())
                .updatedAt(stage.getUpdatedAt())
                .build();
    }
}
