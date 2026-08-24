package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.*;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.exception.InvalidEventStatusTransitionException;
import com.nsbm.eventmanagementservice.exception.VenueNotFoundException;
import com.nsbm.eventmanagementservice.mapper.EventMapper;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.model.EventStatus;
import com.nsbm.eventmanagementservice.model.Venue;
import com.nsbm.eventmanagementservice.repository.EventRepository;
import com.nsbm.eventmanagementservice.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event event;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(1L)
                .title("Industry Panel")
                .status(EventStatus.DRAFT)
                .startDateTime(LocalDateTime.now().plusDays(5))
                .build();

        eventResponse = EventResponse.builder()
                .id(1L)
                .title("Industry Panel")
                .status(EventStatus.DRAFT)
                .build();
    }

    @Test
    void createEvent_withoutVenue_savesSuccessfully() {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Industry Panel")
                .startDateTime(LocalDateTime.now().plusDays(5))
                .build();

        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        EventResponse result = eventService.createEvent(request);

        assertThat(result.getTitle()).isEqualTo("Industry Panel");
        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository).save(any(Event.class));
        verify(venueRepository, never()).findById(any());
    }

    @Test
    void createEvent_withVenue_looksUpAndAttachesVenue() {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Industry Panel")
                .startDateTime(LocalDateTime.now().plusDays(5))
                .venueId(10L)
                .build();

        Venue venue = Venue.builder().id(10L).name("Auditorium A").build();

        when(eventMapper.toEntity(request)).thenReturn(event);
        when(venueRepository.findById(10L)).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        eventService.createEvent(request);

        verify(venueRepository).findById(10L);
        assertThat(event.getVenue()).isEqualTo(venue);
    }

    @Test
    void createEvent_withInvalidVenue_throwsVenueNotFoundException() {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Industry Panel")
                .startDateTime(LocalDateTime.now().plusDays(5))
                .venueId(999L)
                .build();

        when(eventMapper.toEntity(request)).thenReturn(event);
        when(venueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(VenueNotFoundException.class)
                .hasMessageContaining("999");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void getEventById_whenExists_returnsEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        EventResponse result = eventService.getEventById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getEventById_whenNotFound_throwsEventNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(999L))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateStatus_validTransition_draftToScheduled_succeeds() {
        UpdateEventStatusRequest request = UpdateEventStatusRequest.builder()
                .status(EventStatus.SCHEDULED)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        eventService.updateStatus(1L, request);

        assertThat(event.getStatus()).isEqualTo(EventStatus.SCHEDULED);
        verify(eventRepository).save(event);
    }

    @Test
    void updateStatus_invalidTransition_draftToCompleted_throwsException() {
        UpdateEventStatusRequest request = UpdateEventStatusRequest.builder()
                .status(EventStatus.COMPLETED)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateStatus(1L, request))
                .isInstanceOf(InvalidEventStatusTransitionException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateStatus_fromTerminalStatus_completed_throwsException() {
        event.setStatus(EventStatus.COMPLETED);
        UpdateEventStatusRequest request = UpdateEventStatusRequest.builder()
                .status(EventStatus.SCHEDULED)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateStatus(1L, request))
                .isInstanceOf(InvalidEventStatusTransitionException.class);
    }

    @Test
    void assignCoordinator_setsCoordinatorFieldsOnEvent() {
        AssignCoordinatorRequest request = AssignCoordinatorRequest.builder()
                .coordinatorUserId(42L)
                .coordinatorName("Ashini Fernando")
                .coordinatorEmail("ashini@example.com")
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        eventService.assignCoordinator(1L, request);

        assertThat(event.getCoordinatorUserId()).isEqualTo(42L);
        assertThat(event.getCoordinatorName()).isEqualTo("Ashini Fernando");
        assertThat(event.getCoordinatorEmail()).isEqualTo("ashini@example.com");
    }

    @Test
    void removeCoordinator_clearsCoordinatorFields() {
        event.setCoordinatorUserId(42L);
        event.setCoordinatorName("Ashini Fernando");
        event.setCoordinatorEmail("ashini@example.com");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(eventResponse);

        eventService.removeCoordinator(1L);

        assertThat(event.getCoordinatorUserId()).isNull();
        assertThat(event.getCoordinatorName()).isNull();
        assertThat(event.getCoordinatorEmail()).isNull();
    }

    @Test
    void deleteEvent_whenExists_deletesSuccessfully() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        eventService.deleteEvent(1L);

        verify(eventRepository).delete(event);
    }

    @Test
    void deleteEvent_whenNotFound_throwsException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(999L))
                .isInstanceOf(EventNotFoundException.class);

        verify(eventRepository, never()).delete(any());
    }
}
