package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;
import com.nsbm.eventmanagementservice.exception.AgendaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.mapper.AgendaMapper;
import com.nsbm.eventmanagementservice.model.Agenda;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.repository.AgendaRepository;
import com.nsbm.eventmanagementservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgendaServiceImplTest {

    @Mock
    private AgendaRepository agendaRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AgendaMapper agendaMapper;

    @InjectMocks
    private AgendaServiceImpl agendaService;

    private Agenda agenda;
    private AgendaResponse agendaResponse;
    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder().id(1L).title("Industry Panel").build();

        agenda = Agenda.builder()
                .id(1L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        agendaResponse = AgendaResponse.builder()
                .id(1L)
                .title("Opening Keynote")
                .eventId(1L)
                .build();
    }

    @Test
    void createAgendaItem_withValidEvent_savesSuccessfully() {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(1L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaMapper.toEntity(request)).thenReturn(agenda);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(agendaRepository.save(agenda)).thenReturn(agenda);
        when(agendaMapper.toResponse(agenda)).thenReturn(agendaResponse);

        AgendaResponse result = agendaService.createAgendaItem(request);

        assertThat(result.getTitle()).isEqualTo("Opening Keynote");
        assertThat(agenda.getEvent()).isEqualTo(event);
    }

    @Test
    void createAgendaItem_withInvalidEvent_throwsEventNotFoundException() {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(999L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaMapper.toEntity(request)).thenReturn(agenda);
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.createAgendaItem(request))
                .isInstanceOf(EventNotFoundException.class);

        verify(agendaRepository, never()).save(any());
    }

    @Test
    void getAgendaItemById_whenNotFound_throwsException() {
        when(agendaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.getAgendaItemById(999L))
                .isInstanceOf(AgendaNotFoundException.class);
    }

    @Test
    void getAgendaByEventId_returnsOrderedList() {
        when(agendaRepository.findByEventIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(agenda));
        when(agendaMapper.toResponse(agenda)).thenReturn(agendaResponse);

        List<AgendaResponse> result = agendaService.getAgendaByEventId(1L);

        assertThat(result).hasSize(1);
        verify(agendaRepository).findByEventIdOrderBySequenceOrderAsc(1L);
    }

    @Test
    void getAgendaBySpeakerId_returnsEmptyList() {
        List<AgendaResponse> result = agendaService.getAgendaBySpeakerId(2L);
        assertThat(result).isEmpty();
    }

    @Test
    void deleteAgendaItem_whenExists_deletes() {
        when(agendaRepository.findById(1L)).thenReturn(Optional.of(agenda));

        agendaService.deleteAgendaItem(1L);

        verify(agendaRepository).delete(agenda);
    }

    @Test
    void deleteAgendaItem_whenNotFound_throwsException() {
        when(agendaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.deleteAgendaItem(999L))
                .isInstanceOf(AgendaNotFoundException.class);

        verify(agendaRepository, never()).delete(any());
    }
}
