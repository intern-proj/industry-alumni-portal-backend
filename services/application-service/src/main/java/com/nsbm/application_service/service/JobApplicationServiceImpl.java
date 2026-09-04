package com.nsbm.application_service.service;

import com.nsbm.application_service.client.UserServiceClient;
import com.nsbm.application_service.dto.*;
import com.nsbm.application_service.exception.ResourceNotFoundException;
import com.nsbm.application_service.model.*;
import com.nsbm.application_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.nsbm.application_service.config.RabbitMQConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationStatusAuditRepository auditRepository;
    private final RecruitmentStageRepository recruitmentStageRepository;
    private final UserServiceClient userServiceClient;
    private final com.nsbm.application_service.client.AIServiceClient aiServiceClient;
    private final com.nsbm.application_service.client.VacancyServiceClient vacancyServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:vacancy.exchange}")
    private String exchangeName;

    @Override
    @Transactional
    public JobApplicationResponse createApplication(JobApplicationRequest request) {
        Integer matchPercentage = null;
        String matchedSkillsStr = null;
        String missingSkillsStr = null;
        String fitSummary = null;
        String strongFortesStr = null;
        String scoreBreakdownStr = null;

        // 1. Fetch Candidate Profile & Verified Skills
        String candidateName = null;
        String candidateEmail = null;
        String candidateFaculty = null;
        List<String> candidateSkills = new ArrayList<>();

        try {
            if (request.getAlumniId() != null) {
                UserApiResponseDto userRes = userServiceClient.getUserProfile(request.getAlumniId().toString());
                UserProfileDto userProfile = (userRes != null && userRes.getData() != null) ? userRes.getData() : null;
                if (userProfile != null) {
                    candidateName = userProfile.getFullName();
                    candidateEmail = userProfile.getEmail();
                    candidateFaculty = userProfile.getFaculty();
                    if (userProfile.getSkills() != null) {
                        for (UserProfileDto.SkillDto s : userProfile.getSkills()) {
                            if (s.getSkillName() != null && !s.getSkillName().trim().isEmpty()) {
                                candidateSkills.add(s.getSkillName().trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to pre-fetch candidate profile during application creation: {}", e.getMessage());
        }

        // 2. Fetch Vacancy Requirements & Details
        String vacTitle = request.getVacancyTitle();
        String vacReqs = request.getVacancyRequirements();
        String vacDesc = request.getVacancyDescription();
        String vacTags = request.getVacancyTags();

        if (vacTitle == null || vacReqs == null) {
            try {
                com.nsbm.application_service.dto.VacancyApiResponseDto vacRes = vacancyServiceClient.getVacancyById(request.getVacancyId());
                if (vacRes != null && vacRes.getData() != null) {
                    com.nsbm.application_service.dto.VacancyDetailDto vacData = vacRes.getData();
                    vacTitle = vacData.getTitle();
                    vacReqs = vacData.getRequirements();
                    vacDesc = vacData.getDescription();
                    vacTags = vacData.getTags();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch vacancy details from vacancy-service: {}", e.getMessage());
            }
        }

        if (vacTitle == null) {
            vacTitle = "Job Vacancy #" + request.getVacancyId();
        }

        // 3. AI match processing is handled asynchronously via RabbitMQ queue
        UUID alumniUuid = null;
        if (request.getAlumniId() != null) {
            try {
                alumniUuid = UUID.fromString(request.getAlumniId());
            } catch (Exception e) {
                alumniUuid = UUID.nameUUIDFromBytes(request.getAlumniId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        String finalStudentName = request.getStudentName() != null ? request.getStudentName() : (candidateName != null ? candidateName : "Student Candidate");
        String finalStudentEmail = request.getStudentEmail() != null ? request.getStudentEmail() : (candidateEmail != null ? candidateEmail : "student@students.nsbm.ac.lk");
        String finalProgram = request.getProgram() != null ? request.getProgram() : "Faculty of Computing";
        String finalGpa = request.getGpa() != null ? request.getGpa() : "3.8";

        JobApplication application = JobApplication.builder()
                .vacancyId(request.getVacancyId())
                .alumniId(alumniUuid)
                .resumeUrl(request.getResumeUrl())
                .coverLetter(request.getCoverLetter())
                .studentName(finalStudentName)
                .studentEmail(finalStudentEmail)
                .program(finalProgram)
                .gpa(finalGpa)
                .profilePicUrl(request.getProfilePicUrl())
                .matchPercentage(matchPercentage)
                .matchedSkills(matchedSkillsStr)
                .missingSkills(missingSkillsStr)
                .fitSummary(fitSummary)
                .strongFortes(strongFortesStr)
                .scoreBreakdown(scoreBreakdownStr)
                .status(ApplicationStatus.PENDING)
                .build();

        JobApplication saved = jobApplicationRepository.save(application);

        ApplicationStatusAudit audit = ApplicationStatusAudit.builder()
                .applicationId(saved.getId())
                .previousStatus(null)
                .newStatus(ApplicationStatus.PENDING)
                .changedBy("SYSTEM")
                .changeReason("Initial application intake with AI ATS evaluation")
                .build();

        auditRepository.save(audit);

        // Queue RabbitMQ event for async AI evaluation (processed when AI service is online)
        try {
            ApplicationSubmittedEvent event = ApplicationSubmittedEvent.builder()
                    .applicationId(saved.getId().toString())
                    .vacancyId(saved.getVacancyId())
                    .alumniId(saved.getAlumniId() != null ? saved.getAlumniId().toString() : null)
                    .resumeUrl(saved.getResumeUrl())
                    .coverLetter(saved.getCoverLetter())
                    .vacancyTitle(vacTitle)
                    .vacancyRequirements(vacReqs)
                    .vacancyDescription(vacDesc)
                    .vacancyTags(vacTags)
                    .candidateName(candidateName)
                    .candidateEmail(candidateEmail)
                    .candidateFaculty(candidateFaculty)
                    .candidateSkills(candidateSkills)
                    .build();

            rabbitTemplate.convertAndSend(exchangeName, RabbitMQConfig.APPLICATION_SUBMITTED_ROUTING_KEY, event);
            log.info("Queued RabbitMQ message for AI evaluation of application ID: {}", saved.getId());
        } catch (Exception e) {
            log.warn("RabbitMQ queueing deferred or failed for application {}: {}", saved.getId(), e.getMessage());
        }

        return mapToResponse(saved);
    }

    @Override
    public JobApplicationResponse getApplicationById(UUID id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with id: " + id));
        return mapToResponse(application);
    }

    @Override
    public List<JobApplicationResponse> getAllApplications() {
        return jobApplicationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

    @Override
    @Transactional
    public void deleteApplication(UUID id, UUID alumniId) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with id: " + id));

        if (!application.getAlumniId().equals(alumniId)) {
            throw new IllegalArgumentException("Unauthorized to delete this application");
        }

        // Delete related audits
        List<ApplicationStatusAudit> audits = auditRepository.findByApplicationIdOrderByChangedAtDesc(id);
        auditRepository.deleteAll(audits);

        // Delete related stages
        List<RecruitmentStage> stages = recruitmentStageRepository.findByApplicationIdOrderByCreatedAtAsc(id);
        recruitmentStageRepository.deleteAll(stages);

        // Delete application
        jobApplicationRepository.delete(application);
    }

    @Override
    @Transactional
    public JobApplicationResponse updateAiInsights(UUID id, AiInsightsUpdateRequest request) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with id: " + id));

        if (request.getMatchPercentage() != null) {
            application.setMatchPercentage(request.getMatchPercentage());
        }
        if (request.getMatchedSkills() != null) {
            application.setMatchedSkills(request.getMatchedSkills());
        }
        if (request.getMissingSkills() != null) {
            application.setMissingSkills(request.getMissingSkills());
        }
        if (request.getFitSummary() != null) {
            application.setFitSummary(request.getFitSummary());
        }
        if (request.getStrongFortes() != null) {
            application.setStrongFortes(request.getStrongFortes());
        }
        if (request.getScoreBreakdown() != null) {
            application.setScoreBreakdown(request.getScoreBreakdown());
        }

        JobApplication saved = jobApplicationRepository.save(application);
        log.info("Successfully persisted AI match insights for application ID: {}", id);
        return mapToResponse(saved);
    }

    private JobApplicationResponse mapToResponse(JobApplication application) {
        JobApplicationResponse response = JobApplicationResponse.builder()
                .id(application.getId())
                .vacancyId(application.getVacancyId())
                .alumniId(application.getAlumniId())
                .resumeUrl(application.getResumeUrl())
                .coverLetter(application.getCoverLetter())
                .studentName(application.getStudentName() != null ? application.getStudentName() : "Student Candidate")
                .studentEmail(application.getStudentEmail() != null ? application.getStudentEmail() : "student@students.nsbm.ac.lk")
                .program(application.getProgram() != null ? application.getProgram() : "Faculty of Computing")
                .gpa(application.getGpa() != null ? application.getGpa() : "3.8")
                .profilePicUrl(application.getProfilePicUrl())
                .matchPercentage(application.getMatchPercentage())
                .matchedSkills(application.getMatchedSkills())
                .missingSkills(application.getMissingSkills())
                .fitSummary(application.getFitSummary())
                .strongFortes(application.getStrongFortes())
                .scoreBreakdown(application.getScoreBreakdown())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
                
        try {
            if (application.getAlumniId() != null) {
                UserApiResponseDto userRes = userServiceClient.getUserProfile(application.getAlumniId().toString());
                UserProfileDto userProfile = (userRes != null && userRes.getData() != null) ? userRes.getData() : null;
                if (userProfile != null) {
                    response.setStudentName(userProfile.getFullName());
                    response.setStudentEmail(userProfile.getEmail());
                    String prog = userProfile.getDepartment() != null ? userProfile.getDepartment() : userProfile.getFaculty();
                    if (userProfile.getAcademicRecord() != null && userProfile.getAcademicRecord().getDegreeProgram() != null) {
                        prog = userProfile.getAcademicRecord().getDegreeProgram();
                    }
                    response.setProgram(prog);
                    if (userProfile.getAcademicRecord() != null && userProfile.getAcademicRecord().getGpa() != null) {
                        response.setGpa(userProfile.getAcademicRecord().getGpa().toString());
                    }
                    if (userProfile.getProfilePicUrl() != null && !userProfile.getProfilePicUrl().isBlank()) {
                        response.setProfilePicUrl(userProfile.getProfilePicUrl());
                    }
                }
            }
        } catch (feign.FeignException.NotFound e) {
            log.warn("User profile not found for alumniId: {}", application.getAlumniId());
        } catch (Exception e) {
            log.warn("Failed to fetch user profile for alumniId: {} - {}", application.getAlumniId(), e.getMessage());
        }
        
        return response;
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
