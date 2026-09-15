package com.nsbm.application_service.controller;

import com.nsbm.application_service.dto.*;
import com.nsbm.application_service.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @Valid @RequestBody JobApplicationRequest request) {
        JobApplicationResponse response = jobApplicationService.createApplication(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getAllApplications(
            @RequestParam(required = false) Long vacancyId,
            @RequestParam(required = false) String alumniId) {
        if (vacancyId != null) {
            return ResponseEntity.ok(jobApplicationService.getApplicationsByVacancyId(vacancyId));
        }
        if (alumniId != null) {
            UUID parsedUuid;
            try {
                parsedUuid = UUID.fromString(alumniId);
            } catch (Exception e) {
                parsedUuid = UUID.nameUUIDFromBytes(alumniId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return ResponseEntity.ok(jobApplicationService.getApplicationsByAlumniId(parsedUuid));
        }
        return ResponseEntity.ok(jobApplicationService.getAllApplications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplicationResponse> getApplicationById(@PathVariable UUID id) {
        JobApplicationResponse response = jobApplicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vacancy/{vacancyId}")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByVacancyId(
            @PathVariable Long vacancyId) {
        List<JobApplicationResponse> responses = jobApplicationService.getApplicationsByVacancyId(vacancyId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/alumni/{alumniId}")
    public ResponseEntity<List<JobApplicationResponse>> getApplicationsByAlumniId(
            @PathVariable String alumniId) {
        UUID parsedUuid;
        try {
            parsedUuid = UUID.fromString(alumniId);
        } catch (Exception e) {
            parsedUuid = UUID.nameUUIDFromBytes(alumniId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        List<JobApplicationResponse> responses = jobApplicationService.getApplicationsByAlumniId(parsedUuid);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusChangeRequest request) {
        JobApplicationResponse response = jobApplicationService.updateApplicationStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/ai-insights")
    public ResponseEntity<JobApplicationResponse> updateAiInsights(
            @PathVariable UUID id,
            @RequestBody AiInsightsUpdateRequest request) {
        JobApplicationResponse response = jobApplicationService.updateAiInsights(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/audits")
    public ResponseEntity<List<StatusAuditResponse>> getStatusAudits(@PathVariable UUID id) {
        List<StatusAuditResponse> audits = jobApplicationService.getStatusAudits(id);
        return ResponseEntity.ok(audits);
    }

    @PostMapping("/{id}/stages")
    public ResponseEntity<RecruitmentStageResponse> scheduleStage(
            @PathVariable UUID id,
            @Valid @RequestBody RecruitmentStageRequest request) {
        RecruitmentStageResponse response = jobApplicationService.scheduleStage(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/stages/{stageId}")
    public ResponseEntity<RecruitmentStageResponse> updateStage(
            @PathVariable UUID id,
            @PathVariable UUID stageId,
            @Valid @RequestBody StageUpdateRequest request) {
        RecruitmentStageResponse response = jobApplicationService.updateStage(id, stageId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/stages")
    public ResponseEntity<List<RecruitmentStageResponse>> getStages(@PathVariable UUID id) {
        List<RecruitmentStageResponse> stages = jobApplicationService.getStages(id);
        return ResponseEntity.ok(stages);
    }

    @DeleteMapping("/{id}/alumni/{alumniId}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable UUID id,
            @PathVariable String alumniId) {
        UUID parsedUuid;
        try {
            parsedUuid = UUID.fromString(alumniId);
        } catch (Exception e) {
            parsedUuid = UUID.nameUUIDFromBytes(alumniId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        jobApplicationService.deleteApplication(id, parsedUuid);
        return ResponseEntity.noContent().build();
    }
}
