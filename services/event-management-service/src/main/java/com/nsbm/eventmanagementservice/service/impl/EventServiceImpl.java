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
    private final EventMapper eventMapper;

    private static final Map<EventStatus, Set<EventStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(EventStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(EventStatus.DRAFT, EnumSet.of(EventStatus.SCHEDULED, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.SCHEDULED, EnumSet.of(EventStatus.ONGOING, EventStatus.RESCHEDULED, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.RESCHEDULED, EnumSet.of(EventStatus.SCHEDULED, EventStatus.ONGOING, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.ONGOING, EnumSet.of(EventStatus.COMPLETED, EventStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(EventStatus.COMPLETED, EnumSet.noneOf(EventStatus.class));
        ALLOWED_TRANSITIONS.put(EventStatus.CANCELLED, EnumSet.noneOf(EventStatus.class));
    }

    @Override
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = eventMapper.toEntity(request);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException(request.getVenueId()));
            event.setVenue(venue);
        }

        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = findEventOrThrow(id);
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByStatus(EventStatus status) {
        return eventRepository.findByStatus(status).stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByVenue(Long venueId) {
        return eventRepository.findByVenueId(venueId).stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByCoordinator(Long coordinatorUserId) {
        return eventRepository.findByCoordinatorUserId(coordinatorUserId).stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = findEventOrThrow(id);

        eventMapper.updateEntityFromRequest(request, event);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException(request.getVenueId()));
            event.setVenue(venue);
        }

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    public EventResponse updateStatus(Long id, UpdateEventStatusRequest request) {
        Event event = findEventOrThrow(id);
        transitionStatus(event, request.getStatus());
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    public EventResponse rescheduleEvent(Long id, RescheduleEventRequest request) {
        Event event = findEventOrThrow(id);
        transitionStatus(event, EventStatus.RESCHEDULED);

        event.setStartDateTime(request.getNewStartDateTime());
        event.setEndDateTime(request.getNewEndDateTime());

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    public EventResponse cancelEvent(Long id) {
        Event event = findEventOrThrow(id);
        transitionStatus(event, EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
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
        return eventMapper.toResponse(saved);
    }

    @Override
    public EventResponse removeCoordinator(Long id) {
        Event event = findEventOrThrow(id);

        event.setCoordinatorUserId(null);
        event.setCoordinatorName(null);
        event.setCoordinatorEmail(null);

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    private Event findEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    private void transitionStatus(Event event, EventStatus targetStatus) {
        EventStatus currentStatus = event.getStatus();
        Set<EventStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(EventStatus.class));

        if (!allowed.contains(targetStatus)) {
            throw new InvalidEventStatusTransitionException(currentStatus, targetStatus);
        }

        event.setStatus(targetStatus);
    }
}
