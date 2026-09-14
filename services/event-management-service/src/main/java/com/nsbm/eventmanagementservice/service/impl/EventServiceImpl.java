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
import com.nsbm.eventmanagementservice.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nsbm.eventmanagementservice.mapper.AgendaMapper;
import com.nsbm.eventmanagementservice.model.Agenda;
import com.nsbm.eventmanagementservice.repository.GuestSpeakerRepository;
import com.nsbm.eventmanagementservice.exception.ResourceNotFoundException;
import com.nsbm.eventmanagementservice.model.Lecture;
import java.util.ArrayList;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final GuestSpeakerRepository guestSpeakerRepository;
    private final EventMapper eventMapper;
    private final AgendaMapper agendaMapper;

    private static final Map<EventStatus, Set<EventStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(EventStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(EventStatus.DRAFT, EnumSet.of(EventStatus.SCHEDULED, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.SCHEDULED, EnumSet.of(EventStatus.DRAFT, EventStatus.ONGOING, EventStatus.RESCHEDULED, EventStatus.CANCELLED, EventStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(EventStatus.RESCHEDULED, EnumSet.of(EventStatus.SCHEDULED, EventStatus.ONGOING, EventStatus.DRAFT, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.ONGOING, EnumSet.of(EventStatus.COMPLETED, EventStatus.SCHEDULED, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.COMPLETED, EnumSet.of(EventStatus.SCHEDULED, EventStatus.DRAFT));
        ALLOWED_TRANSITIONS.put(EventStatus.CANCELLED, EnumSet.of(EventStatus.DRAFT, EventStatus.SCHEDULED));
    }

    @Override
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = eventMapper.toEntity(request);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException(request.getVenueId()));
            event.setVenue(venue);
        }

        event.setStatus(request.getStatus() != null ? request.getStatus() : EventStatus.DRAFT);

        if (request.getSessions() != null && !request.getSessions().isEmpty()) {
            List<Agenda> agendas = new ArrayList<>();
            for (AgendaRequest sessionReq : request.getSessions()) {
                Agenda agenda = agendaMapper.toEntity(sessionReq);
                agenda.setEvent(event);
                if (sessionReq.getVenueId() != null) {
                    Venue agendaVenue = venueRepository.findById(sessionReq.getVenueId())
                            .orElseThrow(() -> new VenueNotFoundException(sessionReq.getVenueId()));
                    agenda.setVenue(agendaVenue);
                    if (event.getVenue() == null) {
                        event.setVenue(agendaVenue);
                    }
                }
                
                if (sessionReq.getLectures() != null && !sessionReq.getLectures().isEmpty()) {
                    List<Lecture> lectures = new ArrayList<>();
                    for (LectureRequest lectureReq : sessionReq.getLectures()) {
                        Lecture lecture = Lecture.builder()
                                .agenda(agenda)
                                .title(lectureReq.getTitle())
                                .description(lectureReq.getDescription())
                                .startTime(lectureReq.getStartTime())
                                .endTime(lectureReq.getEndTime())
                                .sequenceOrder(lectureReq.getSequenceOrder())
                                .build();
                                
                        if (lectureReq.getSpeakerId() != null) {
                            lecture.setSpeaker(guestSpeakerRepository.findById(lectureReq.getSpeakerId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Speaker not found with id " + lectureReq.getSpeakerId())));
                        }
                        lectures.add(lecture);
                    }
                    agenda.setLectures(lectures);
                }
                
                agendas.add(agenda);
            }
            event.setAgendas(agendas);
        }

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = findEventOrThrow(id);
        return toEventResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByStatus(EventStatus status) {
        return eventRepository.findByStatus(status).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByVenue(Long venueId) {
        return eventRepository.findByVenueId(venueId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByCoordinator(Long coordinatorUserId) {
        return eventRepository.findByCoordinatorUserId(coordinatorUserId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsBySpeakerId(Long speakerId) {
        return eventRepository.findDistinctByAgendasSpeakerId(speakerId).stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = findEventOrThrow(id);

        eventMapper.updateEntityFromRequest(request, event);

        if (request.getRequiredAttendanceRate() != null) {
            event.setRequiredAttendanceRate(request.getRequiredAttendanceRate());
        }

        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException(request.getVenueId()));
            event.setVenue(venue);
        }

        if (request.getSessions() != null) {
            if (event.getAgendas() == null) {
                event.setAgendas(new ArrayList<>());
            } else {
                event.getAgendas().clear();
            }

            for (AgendaRequest sessionReq : request.getSessions()) {
                Agenda agenda = agendaMapper.toEntity(sessionReq);
                agenda.setEvent(event);
                if (sessionReq.getVenueId() != null) {
                    Venue agendaVenue = venueRepository.findById(sessionReq.getVenueId())
                            .orElseThrow(() -> new VenueNotFoundException(sessionReq.getVenueId()));
                    agenda.setVenue(agendaVenue);
                    if (event.getVenue() == null) {
                        event.setVenue(agendaVenue);
                    }
                }
                
                if (sessionReq.getLectures() != null && !sessionReq.getLectures().isEmpty()) {
                    List<Lecture> lectures = new ArrayList<>();
                    for (LectureRequest lectureReq : sessionReq.getLectures()) {
                        Lecture lecture = Lecture.builder()
                                .agenda(agenda)
                                .title(lectureReq.getTitle())
                                .description(lectureReq.getDescription())
                                .startTime(lectureReq.getStartTime())
                                .endTime(lectureReq.getEndTime())
                                .sequenceOrder(lectureReq.getSequenceOrder())
                                .build();
                                
                        if (lectureReq.getSpeakerId() != null) {
                            lecture.setSpeaker(guestSpeakerRepository.findById(lectureReq.getSpeakerId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Speaker not found with id " + lectureReq.getSpeakerId())));
                        }
                        lectures.add(lecture);
                    }
                    agenda.setLectures(lectures);
                }
                event.getAgendas().add(agenda);
            }
        }

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Override
    public EventResponse updateStatus(Long id, UpdateEventStatusRequest request) {
        Event event = findEventOrThrow(id);
        transitionStatus(event, request.getStatus());
        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Override
    public EventResponse rescheduleEvent(Long id, RescheduleEventRequest request) {
        Event event = findEventOrThrow(id);
        transitionStatus(event, EventStatus.RESCHEDULED);

        event.setStartDateTime(request.getNewStartDateTime());
        event.setEndDateTime(request.getNewEndDateTime());

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Override
    public EventResponse cancelEvent(Long id) {
        Event event = findEventOrThrow(id);
        transitionStatus(event, EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Override
    public void deleteEvent(Long id) {
        Event event = findEventOrThrow(id);
        eventRepository.delete(event);
    }

    @Override
    public EventResponse assignCoordinator(Long id, AssignCoordinatorRequest request) {
        Event event = findEventOrThrow(id);

        event.setCoordinatorUserId(request.getCoordinatorUserId());
        event.setCoordinatorName(request.getCoordinatorName());
        event.setCoordinatorEmail(request.getCoordinatorEmail());

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Override
    public EventResponse removeCoordinator(Long id) {
        Event event = findEventOrThrow(id);

        event.setCoordinatorUserId(null);
        event.setCoordinatorName(null);
        event.setCoordinatorEmail(null);

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    private Event findEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    private void transitionStatus(Event event, EventStatus targetStatus) {
        EventStatus currentStatus = event.getStatus();
        if (currentStatus == targetStatus) {
            return;
        }
        Set<EventStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(EventStatus.class));

        if (!allowed.contains(targetStatus)) {
            throw new InvalidEventStatusTransitionException(currentStatus, targetStatus);
        }

        event.setStatus(targetStatus);
    }

    private EventResponse toEventResponse(Event event) {
        EventResponse response = eventMapper.toResponse(event);
        if ((response.getVenueName() == null || response.getVenueName().isBlank()) 
                && event.getAgendas() != null && !event.getAgendas().isEmpty()) {
            for (Agenda a : event.getAgendas()) {
                if (a.getVenue() != null && a.getVenue().getName() != null && !a.getVenue().getName().isBlank()) {
                    response.setVenueName(a.getVenue().getName());
                    if (response.getVenueId() == null) {
                        response.setVenueId(a.getVenue().getId());
                    }
                    break;
                }
            }
        }
        return response;
    }
}
