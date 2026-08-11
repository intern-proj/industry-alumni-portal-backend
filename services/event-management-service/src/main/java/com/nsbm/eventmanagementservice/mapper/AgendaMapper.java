package com.nsbm.eventmanagementservice.mapper;

import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;
import com.nsbm.eventmanagementservice.model.Agenda;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AgendaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "speaker", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Agenda toEntity(AgendaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "speaker", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(AgendaRequest request, @MappingTarget Agenda agenda);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "speakerId", source = "speaker.id")
    @Mapping(target = "speakerName", source = "speaker.fullName")
    AgendaResponse toResponse(Agenda agenda);
}
