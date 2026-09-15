package com.nsbm.audit_storage_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsbm.audit_storage_service.dto.AuditLogRequest;
import com.nsbm.audit_storage_service.dto.AuditLogResponse;
import com.nsbm.audit_storage_service.exception.GlobalExceptionHandler;
import com.nsbm.audit_storage_service.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    private AuditLogRequest auditLogRequest;
    private AuditLogResponse auditLogResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setConversionService(new DefaultFormattingConversionService())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper().findAndRegisterModules();

        auditLogRequest = AuditLogRequest.builder()
                .userId("user-123")
                .action("LOGIN")
                .ipAddress("10.0.0.5")
                .resourceType("AUTH")
                .resourceId("session-1")
                .details("Successful login")
                .build();

        auditLogResponse = AuditLogResponse.builder()
                .id(UUID.randomUUID())
                .userId("user-123")
                .action("LOGIN")
                .ipAddress("10.0.0.5")
                .timestamp(Instant.parse("2026-08-14T05:00:00Z"))
                .resourceType("AUTH")
                .resourceId("session-1")
                .details("Successful login")
                .build();
    }

    @Test
    void logAction_returnsAccepted() throws Exception {
        when(auditService.logActionAsync(any(AuditLogRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(auditLogResponse));

        mockMvc.perform(post("/api/v1/audit/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auditLogRequest)))
                .andExpect(status().isAccepted());

        verify(auditService, times(1)).logActionAsync(any(AuditLogRequest.class));
    }

    @Test
    void getLogs_returnsPaginatedTrail() throws Exception {
        when(auditService.getLogs(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLogResponse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user-123"))
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(auditService, times(1)).getLogs(any(Pageable.class));
    }

    @Test
    void getLogs_filtersByUserId() throws Exception {
        when(auditService.getLogsByUserId(eq("user-123"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLogResponse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit/logs").param("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user-123"));

        verify(auditService, times(1)).getLogsByUserId(eq("user-123"), any(Pageable.class));
    }

    @Test
    void getLogs_filtersByAction() throws Exception {
        when(auditService.getLogsByAction(eq("LOGIN"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLogResponse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit/logs").param("action", "LOGIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("LOGIN"));

        verify(auditService, times(1)).getLogsByAction(eq("LOGIN"), any(Pageable.class));
    }

    @Test
    void getLogs_filtersByTimestampRange() throws Exception {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        when(auditService.getLogsBetween(eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLogResponse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(auditLogResponse.getId().toString()));

        verify(auditService, times(1)).getLogsBetween(eq(from), eq(to), any(Pageable.class));
    }
}
