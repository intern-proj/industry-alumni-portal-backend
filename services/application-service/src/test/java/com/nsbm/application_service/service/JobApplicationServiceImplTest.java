package com.nsbm.application_service.service;

import com.nsbm.application_service.dto.*;
import com.nsbm.application_service.exception.ResourceNotFoundException;
import com.nsbm.application_service.model.*;
import com.nsbm.application_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private ApplicationStatusAuditRepository auditRepository;

    @Mock
    private RecruitmentStageRepository recruitmentStageRepository;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private JobApplication jobApplication;
    private JobApplicationRequest applicationRequest;
    private UUID applicationId;
    private UUID vacancyId;
    private UUID alumniId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        vacancyId = UUID.randomUUID();
        alumniId = UUID.randomUUID();

        jobApplication = JobApplication.builder()
                .id(applicationId)
                .vacancyId(vacancyId)
                .alumniId(alumniId)
                .resumeUrl("http://example.com/resume.pdf")
                .coverLetter("Here is my cover letter")
                .status(ApplicationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        applicationRequest = JobApplicationRequest.builder()
                .vacancyId(vacancyId)
                .alumniId(alumniId)
                .resumeUrl("http://example.com/resume.pdf")
                .coverLetter("Here is my cover letter")
                .build();
    }

    @Test
    void testCreateApplication() {
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(jobApplication);
        when(auditRepository.save(any(ApplicationStatusAudit.class))).thenReturn(new ApplicationStatusAudit());

        JobApplicationResponse response = jobApplicationService.createApplication(applicationRequest);

        assertNotNull(response);
        assertEquals(applicationId, response.getId());
        assertEquals(ApplicationStatus.PENDING, response.getStatus());
        verify(jobApplicationRepository, times(1)).save(any(JobApplication.class));
        verify(auditRepository, times(1)).save(any(ApplicationStatusAudit.class));
    }

    @Test
    void testGetApplicationById_Success() {
        when(jobApplicationRepository.findById(applicationId)).thenReturn(Optional.of(jobApplication));

        JobApplicationResponse response = jobApplicationService.getApplicationById(applicationId);

        assertNotNull(response);
        assertEquals(applicationId, response.getId());
        verify(jobApplicationRepository, times(1)).findById(applicationId);
    }

    @Test
    void testGetApplicationById_NotFound() {
        when(jobApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            jobApplicationService.getApplicationById(applicationId);
        });
        verify(jobApplicationRepository, times(1)).findById(applicationId);
    }

    @Test
    void testGetApplicationsByVacancyId() {
        when(jobApplicationRepository.findByVacancyId(vacancyId)).thenReturn(Arrays.asList(jobApplication));

        List<JobApplicationResponse> responses = jobApplicationService.getApplicationsByVacancyId(vacancyId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(vacancyId, responses.get(0).getVacancyId());
        verify(jobApplicationRepository, times(1)).findByVacancyId(vacancyId);
    }

    @Test
    void testUpdateApplicationStatus() {
        when(jobApplicationRepository.findById(applicationId)).thenReturn(Optional.of(jobApplication));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(jobApplication);
        when(auditRepository.save(any(ApplicationStatusAudit.class))).thenReturn(new ApplicationStatusAudit());

        StatusChangeRequest changeRequest = StatusChangeRequest.builder()
                .newStatus(ApplicationStatus.SHORTLISTED)
                .changedBy("HR_USER")
                .changeReason("Great background match")
                .build();

        JobApplicationResponse response = jobApplicationService.updateApplicationStatus(applicationId, changeRequest);

        assertNotNull(response);
        assertEquals(ApplicationStatus.SHORTLISTED, response.getStatus());
        verify(jobApplicationRepository, times(1)).findById(applicationId);
        verify(jobApplicationRepository, times(1)).save(any(JobApplication.class));
        verify(auditRepository, times(1)).save(any(ApplicationStatusAudit.class));
    }
}
