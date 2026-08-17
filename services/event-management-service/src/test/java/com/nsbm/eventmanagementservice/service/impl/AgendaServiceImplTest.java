package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;
import com.nsbm.eventmanagementservice.exception.AgendaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.exception.GuestSpeakerNotFoundException;
import com.nsbm.eventmanagementservice.mapper.AgendaMapper;
import com.nsbm.eventmanagementservice.model.Agenda;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.model.GuestSpeaker;
import com.nsbm.eventmanagementservice.repository.AgendaRepository;
import com.nsbm.eventmanagementservice.repository.EventRepository;
import com.nsbm.eventmanagementservice.repository.GuestSpeakerRepository;
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
    private GuestSpeakerRepository guestSpeakerRepository;

    @Mock
    private AgendaMapper agendaMapper;

    @InjectMocks
    private AgendaServiceImpl agendaService;

    private Agenda agenda;
    private AgendaResponse agendaResponse;
    private Event event;
    private GuestSpeaker speaker;

    @BeforeEach
    void setUp() {
        event = Event.builder().id(1L).title("Industry Panel").build();
        speaker = GuestSpeaker.builder().id(2L).fullName("Dr. Nimal Perera").build();

        agenda = Agenda.builder()
                .id(1L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        agendaResponse = AgendaResponse.builder()
                .id(1L)
                .title("Opening Keynote")
                .eventId(1L)
                .speakerId(2L)
                .build();
    }

    @Test
    void createAgendaItem_withValidEventAndSpeaker_savesSuccessfully() {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(1L)
                .speakerId(2L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaMapper.toEntity(request)).thenReturn(agenda);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(guestSpeakerRepository.findById(2L)).thenReturn(Optional.of(speaker));
        when(agendaRepository.save(agenda)).thenReturn(agenda);
        when(agendaMapper.toResponse(agenda)).thenReturn(agendaResponse);

        AgendaResponse result = agendaService.createAgendaItem(request);

        assertThat(result.getTitle()).isEqualTo("Opening Keynote");
        assertThat(agenda.getEvent()).isEqualTo(event);
        assertThat(agenda.getSpeaker()).isEqualTo(speaker);
    }

    @Test
    void createAgendaItem_withoutSpeaker_savesWithoutSpeakerLookup() {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(1L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaMapper.toEntity(request)).thenReturn(agenda);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(agendaRepository.save(agenda)).thenReturn(agenda);
        when(agendaMapper.toResponse(agenda)).thenReturn(agendaResponse);

        agendaService.createAgendaItem(request);

        verify(guestSpeakerRepository, never()).findById(any());
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
    void createAgendaItem_withInvalidSpeaker_throwsGuestSpeakerNotFoundException() {
        AgendaRequest request = AgendaRequest.builder()
                .eventId(1L)
                .speakerId(999L)
                .title("Opening Keynote")
                .startTime(LocalDateTime.now().plusDays(5))
                .build();

        when(agendaMapper.toEntity(request)).thenReturn(agenda);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(guestSpeakerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendaService.createAgendaItem(request))
                .isInstanceOf(GuestSpeakerNotFoundException.class);

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
    void getAgendaBySpeakerId_returnsFilteredList() {
        when(agendaRepository.findBySpeakerId(2L)).thenReturn(List.of(agenda));
        when(agendaMapper.toResponse(agenda)).thenReturn(agendaResponse);

        List<AgendaResponse> result = agendaService.getAgendaBySpeakerId(2L);

        assertThat(result).hasSize(1);
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
