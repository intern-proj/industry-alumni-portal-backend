package com.nsbm.eventmanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaRequest;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaResponse;
import com.nsbm.eventmanagementservice.exception.CertificateEligibilityCriteriaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.exception.GlobalExceptionHandler;
import com.nsbm.eventmanagementservice.service.CertificateEligibilityCriteriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CertificateEligibilityCriteriaControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CertificateEligibilityCriteriaService criteriaService;

    @InjectMocks
    private CertificateEligibilityCriteriaController criteriaController;

    private CertificateEligibilityCriteriaResponse criteriaResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(criteriaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        criteriaResponse = CertificateEligibilityCriteriaResponse.builder()
                .id(1L)
                .eventId(1L)
                .minAttendancePercentage(80)
                .requiresFeedbackSubmission(true)
                .build();
    }

    @Test
    void createOrUpdateCriteria_returns200() throws Exception {
        CertificateEligibilityCriteriaRequest request = CertificateEligibilityCriteriaRequest.builder()
                .minAttendancePercentage(80)
                .requiresFeedbackSubmission(true)
                .build();

        when(criteriaService.createOrUpdateCriteria(eq(1L), any(CertificateEligibilityCriteriaRequest.class)))
                .thenReturn(criteriaResponse);

        mockMvc.perform(put("/api/v1/events/{eventId}/eligibility-criteria", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minAttendancePercentage").value(80));
    }

    @Test
    void createOrUpdateCriteria_withInvalidEvent_returns404() throws Exception {
        CertificateEligibilityCriteriaRequest request = CertificateEligibilityCriteriaRequest.builder()
                .minAttendancePercentage(80)
                .build();

        when(criteriaService.createOrUpdateCriteria(eq(999L), any(CertificateEligibilityCriteriaRequest.class)))
                .thenThrow(new EventNotFoundException(999L));

        mockMvc.perform(put("/api/v1/events/{eventId}/eligibility-criteria", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCriteria_whenExists_returns200() throws Exception {
        when(criteriaService.getCriteriaByEventId(1L)).thenReturn(criteriaResponse);

        mockMvc.perform(get("/api/v1/events/{eventId}/eligibility-criteria", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(1));
    }

    @Test
    void getCriteria_whenNotFound_returns404() throws Exception {
        when(criteriaService.getCriteriaByEventId(999L))
                .thenThrow(new CertificateEligibilityCriteriaNotFoundException(999L));

        mockMvc.perform(get("/api/v1/events/{eventId}/eligibility-criteria", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCriteria_returns204() throws Exception {
        doNothing().when(criteriaService).deleteCriteria(1L);

        mockMvc.perform(delete("/api/v1/events/{eventId}/eligibility-criteria", 1L))
                .andExpect(status().isNoContent());
    }
}
