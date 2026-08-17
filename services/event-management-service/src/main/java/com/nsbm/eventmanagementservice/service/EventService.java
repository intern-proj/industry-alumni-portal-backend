package com.nsbm.eventmanagementservice.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventMapper eventMapper;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        log.info("Creating event: {}", request.getTitle());
        Event event = eventMapper.toEntity(request);
        event.setStatus(EventStatus.SCHEDULED);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException(request.getVenueId()));
            event.setVenue(venue);
        }

        Event saved = eventRepository.save(event);
        log.info("Event created with ID: {}", saved.getId());
        return eventMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
        return eventMapper.toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(EventStatus status) {
        List<Event> events = (status != null) ? eventRepository.findByStatus(status) : eventRepository.findAll();
        return events.stream().map(eventMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        log.info("Updating event with ID: {}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        eventMapper.updateEntityFromRequest(request, event);

        if (request.getVenueId() != null) {
            Venue venue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new VenueNotFoundException(request.getVenueId()));
            event.setVenue(venue);
        }

        Event updated = eventRepository.save(event);
        return eventMapper.toResponse(updated);
    }

    @Transactional
    public EventResponse rescheduleEvent(Long id, RescheduleEventRequest request) {
        log.info("Rescheduling event with ID: {}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        if (event.getStatus() == EventStatus.COMPLETED || event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventStatusTransitionException(event.getStatus(), EventStatus.RESCHEDULED);
        }

        event.setStartDateTime(request.getNewStartDateTime());
        if (request.getNewEndDateTime() != null) {
            event.setEndDateTime(request.getNewEndDateTime());
        }
        event.setStatus(EventStatus.RESCHEDULED);

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public EventResponse updateStatus(Long id, UpdateEventStatusRequest request) {
        log.info("Updating status for event ID: {} to {}", id, request.getStatus());
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        event.setStatus(request.getStatus());
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public EventResponse assignCoordinator(Long id, AssignCoordinatorRequest request) {
        log.info("Assigning coordinator {} to event ID: {}", request.getCoordinatorName(), id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));

        event.setCoordinatorUserId(request.getCoordinatorUserId());
        event.setCoordinatorName(request.getCoordinatorName());
        event.setCoordinatorEmail(request.getCoordinatorEmail());

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Transactional
    public void deleteEvent(Long id) {
        log.info("Deleting event with ID: {}", id);
        if (!eventRepository.existsById(id)) {
            throw new EventNotFoundException(id);
        }
        eventRepository.deleteById(id);
    }
}
