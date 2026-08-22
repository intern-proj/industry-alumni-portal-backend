package com.nsbm.eventmanagementservice.mapper;

import com.nsbm.eventmanagementservice.dto.CreateEventRequest;
import com.nsbm.eventmanagementservice.dto.EventResponse;
import com.nsbm.eventmanagementservice.dto.UpdateEventRequest;
import com.nsbm.eventmanagementservice.model.Event;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "coordinatorUserId", ignore = true)
    @Mapping(target = "coordinatorName", ignore = true)
    @Mapping(target = "coordinatorEmail", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toEntity(CreateEventRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "coordinatorUserId", ignore = true)
    @Mapping(target = "coordinatorName", ignore = true)
    @Mapping(target = "coordinatorEmail", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateEventRequest request, @MappingTarget Event event);

    @Mapping(target = "venueId", source = "venue.id")
    EventResponse toResponse(Event event);
}
