package com.nsbm.audit_storage_service.service;

import com.nsbm.audit_storage_service.dto.AuditLogRequest;
import com.nsbm.audit_storage_service.dto.AuditLogResponse;
import com.nsbm.audit_storage_service.model.AuditLog;
import com.nsbm.audit_storage_service.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    private AuditLogRequest auditLogRequest;
    private AuditLog auditLog;
    private UUID auditId;
    private Instant timestamp;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        auditId = UUID.randomUUID();
        timestamp = Instant.parse("2026-08-14T05:00:00Z");
        pageable = PageRequest.of(0, 20);

        auditLogRequest = AuditLogRequest.builder()
                .userId("user-123")
                .action("FILE_UPLOAD")
                .ipAddress("192.168.1.10")
                .resourceType("STORED_FILE")
                .resourceId("file-1")
                .details("Uploaded resume")
                .build();

        auditLog = AuditLog.builder()
                .id(auditId)
                .userId("user-123")
                .action("FILE_UPLOAD")
                .ipAddress("192.168.1.10")
                .timestamp(timestamp)
                .resourceType("STORED_FILE")
                .resourceId("file-1")
                .details("Uploaded resume")
                .build();
    }

    @Test
    void logAction_persistsImmutableAuditEntry() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(auditLog);

        AuditLogResponse response = auditService.logAction(auditLogRequest);

        assertNotNull(response);
        assertEquals(auditId, response.getId());
        assertEquals("user-123", response.getUserId());
        assertEquals("FILE_UPLOAD", response.getAction());
        assertEquals("192.168.1.10", response.getIpAddress());
        assertEquals(timestamp, response.getTimestamp());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());
        assertEquals("user-123", captor.getValue().getUserId());
        assertEquals("FILE_UPLOAD", captor.getValue().getAction());
        assertEquals("192.168.1.10", captor.getValue().getIpAddress());
        assertNotNull(captor.getValue().getTimestamp());
    }

    @Test
    void logActionAsync_returnsCompletedFuture() throws Exception {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(auditLog);

        CompletableFuture<AuditLogResponse> future = auditService.logActionAsync(auditLogRequest);

        assertNotNull(future);
        AuditLogResponse response = future.get();
        assertEquals(auditId, response.getId());
        assertEquals("FILE_UPLOAD", response.getAction());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void getLogs_returnsPaginatedResults() {
        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));

        Page<AuditLogResponse> page = auditService.getLogs(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals(auditId, page.getContent().getFirst().getId());
        verify(auditLogRepository, times(1)).findAllByOrderByTimestampDesc(pageable);
    }

    @Test
    void getLogsByUserId_filtersByUser() {
        when(auditLogRepository.findByUserIdOrderByTimestampDesc("user-123", pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));

        Page<AuditLogResponse> page = auditService.getLogsByUserId("user-123", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("user-123", page.getContent().getFirst().getUserId());
        verify(auditLogRepository, times(1)).findByUserIdOrderByTimestampDesc("user-123", pageable);
    }

    @Test
    void getLogsByAction_filtersByAction() {
        when(auditLogRepository.findByActionContainingIgnoreCaseOrderByTimestampDesc("upload", pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));

        Page<AuditLogResponse> page = auditService.getLogsByAction("upload", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("FILE_UPLOAD", page.getContent().getFirst().getAction());
        verify(auditLogRepository, times(1))
                .findByActionContainingIgnoreCaseOrderByTimestampDesc("upload", pageable);
    }

    @Test
    void getLogsBetween_filtersByTimestampRange() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");
        when(auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(auditLog), pageable, 1));

        Page<AuditLogResponse> page = auditService.getLogsBetween(from, to, pageable);

        assertEquals(1, page.getTotalElements());
        verify(auditLogRepository, times(1))
                .findByTimestampBetweenOrderByTimestampDesc(eq(from), eq(to), eq(pageable));
    }
}
