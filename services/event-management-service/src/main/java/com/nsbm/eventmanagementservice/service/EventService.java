package com.nsbm.eventmanagementservice.service;

import com.nsbm.eventmanagementservice.dto.*;
import com.nsbm.eventmanagementservice.model.EventStatus;

import java.util.List;

public interface EventService {
    EventResponse createEvent(CreateEventRequest request);

    EventResponse getEventById(Long id);

    List<EventResponse> getAllEvents();

    List<EventResponse> getEventsByStatus(EventStatus status);

    List<EventResponse> getEventsByVenue(Long venueId);

    List<EventResponse> getEventsByCoordinator(Long coordinatorUserId);

    EventResponse updateEvent(Long id, UpdateEventRequest request);

    EventResponse updateStatus(Long id, UpdateEventStatusRequest request);

    EventResponse rescheduleEvent(Long id, RescheduleEventRequest request);

    EventResponse cancelEvent(Long id);

    void deleteEvent(Long id);

    EventResponse assignCoordinator(Long id, AssignCoordinatorRequest request);

    EventResponse removeCoordinator(Long id);
}
