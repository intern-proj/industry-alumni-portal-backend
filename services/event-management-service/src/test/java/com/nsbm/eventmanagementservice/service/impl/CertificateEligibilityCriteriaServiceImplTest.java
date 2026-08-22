package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaRequest;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaResponse;
import com.nsbm.eventmanagementservice.exception.CertificateEligibilityCriteriaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.mapper.CertificateEligibilityCriteriaMapper;
import com.nsbm.eventmanagementservice.model.CertificateEligibilityCriteria;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.repository.CertificateEligibilityCriteriaRepository;
import com.nsbm.eventmanagementservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CertificateEligibilityCriteriaServiceImplTest {

    @Mock
    private CertificateEligibilityCriteriaRepository criteriaRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CertificateEligibilityCriteriaMapper criteriaMapper;

    @InjectMocks
    private CertificateEligibilityCriteriaServiceImpl criteriaService;

    private Event event;
    private CertificateEligibilityCriteria criteria;
    private CertificateEligibilityCriteriaResponse criteriaResponse;

    @BeforeEach
    void setUp() {
        event = Event.builder().id(1L).title("Industry Panel").build();

        criteria = CertificateEligibilityCriteria.builder()
                .id(1L)
                .event(event)
                .minAttendancePercentage(80)
                .requiresFeedbackSubmission(true)
                .build();

        criteriaResponse = CertificateEligibilityCriteriaResponse.builder()
                .id(1L)
                .eventId(1L)
                .minAttendancePercentage(80)
                .requiresFeedbackSubmission(true)
                .build();
    }

    @Test
    void createOrUpdateCriteria_whenNoneExists_createsNew() {
        CertificateEligibilityCriteriaRequest request = CertificateEligibilityCriteriaRequest.builder()
                .minAttendancePercentage(80)
                .requiresFeedbackSubmission(true)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(criteriaRepository.findByEventId(1L)).thenReturn(Optional.empty());
        when(criteriaMapper.toEntity(request)).thenReturn(criteria);
        when(criteriaRepository.save(criteria)).thenReturn(criteria);
        when(criteriaMapper.toResponse(criteria)).thenReturn(criteriaResponse);

        CertificateEligibilityCriteriaResponse result = criteriaService.createOrUpdateCriteria(1L, request);

        assertThat(result.getMinAttendancePercentage()).isEqualTo(80);
        assertThat(criteria.getEvent()).isEqualTo(event);
        verify(criteriaRepository).save(criteria);
    }

    @Test
    void createOrUpdateCriteria_whenExists_updatesExisting() {
        CertificateEligibilityCriteriaRequest request = CertificateEligibilityCriteriaRequest.builder()
                .minAttendancePercentage(90)
                .requiresFeedbackSubmission(false)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(criteriaRepository.findByEventId(1L)).thenReturn(Optional.of(criteria));
        when(criteriaRepository.save(criteria)).thenReturn(criteria);
        when(criteriaMapper.toResponse(criteria)).thenReturn(criteriaResponse);

        criteriaService.createOrUpdateCriteria(1L, request);

        verify(criteriaMapper).updateEntityFromRequest(request, criteria);
        verify(criteriaMapper, never()).toEntity(any());
        verify(criteriaRepository).save(criteria);
    }

    @Test
    void createOrUpdateCriteria_withInvalidEvent_throwsEventNotFoundException() {
        CertificateEligibilityCriteriaRequest request = CertificateEligibilityCriteriaRequest.builder()
                .minAttendancePercentage(80)
                .build();

        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criteriaService.createOrUpdateCriteria(999L, request))
                .isInstanceOf(EventNotFoundException.class);

        verify(criteriaRepository, never()).save(any());
    }

    @Test
    void getCriteriaByEventId_whenExists_returnsCriteria() {
        when(criteriaRepository.findByEventId(1L)).thenReturn(Optional.of(criteria));
        when(criteriaMapper.toResponse(criteria)).thenReturn(criteriaResponse);

        CertificateEligibilityCriteriaResponse result = criteriaService.getCriteriaByEventId(1L);

        assertThat(result.getEventId()).isEqualTo(1L);
    }

    @Test
    void getCriteriaByEventId_whenNotFound_throwsException() {
        when(criteriaRepository.findByEventId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criteriaService.getCriteriaByEventId(999L))
                .isInstanceOf(CertificateEligibilityCriteriaNotFoundException.class);
    }

    @Test
    void deleteCriteria_whenExists_deletes() {
        when(criteriaRepository.findByEventId(1L)).thenReturn(Optional.of(criteria));

        criteriaService.deleteCriteria(1L);

        verify(criteriaRepository).delete(criteria);
    }

    @Test
    void deleteCriteria_whenNotFound_throwsException() {
        when(criteriaRepository.findByEventId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> criteriaService.deleteCriteria(999L))
                .isInstanceOf(CertificateEligibilityCriteriaNotFoundException.class);

        verify(criteriaRepository, never()).delete(any());
    }
}
