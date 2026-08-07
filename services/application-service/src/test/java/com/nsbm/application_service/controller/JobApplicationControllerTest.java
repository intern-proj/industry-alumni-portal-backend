package com.nsbm.application_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.application_service.dto.*;
import com.nsbm.application_service.model.ApplicationStatus;
import com.nsbm.application_service.service.JobApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobApplicationController.class)
public class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    private JobApplicationResponse applicationResponse;
    private JobApplicationRequest applicationRequest;
    private UUID applicationId;
    private UUID vacancyId;
    private UUID alumniId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        vacancyId = UUID.randomUUID();
        alumniId = UUID.randomUUID();

        applicationResponse = JobApplicationResponse.builder()
                .id(applicationId)
                .vacancyId(vacancyId)
                .alumniId(alumniId)
                .resumeUrl("http://example.com/resume.pdf")
                .coverLetter("Cover letter content")
                .status(ApplicationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        applicationRequest = JobApplicationRequest.builder()
                .vacancyId(vacancyId)
                .alumniId(alumniId)
                .resumeUrl("http://example.com/resume.pdf")
                .coverLetter("Cover letter content")
                .build();
    }

    @Test
    void testCreateApplication() throws Exception {
        when(jobApplicationService.createApplication(any(JobApplicationRequest.class))).thenReturn(applicationResponse);

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(applicationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetApplicationById() throws Exception {
        when(jobApplicationService.getApplicationById(applicationId)).thenReturn(applicationResponse);

        mockMvc.perform(get("/api/v1/applications/" + applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
