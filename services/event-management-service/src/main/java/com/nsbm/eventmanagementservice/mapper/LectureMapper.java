package com.nsbm.eventmanagementservice.mapper;

import com.nsbm.eventmanagementservice.dto.LectureRequest;
import com.nsbm.eventmanagementservice.dto.LectureResponse;
import com.nsbm.eventmanagementservice.model.Lecture;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LectureMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agenda", ignore = true)
    @Mapping(target = "speaker", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lecture toEntity(LectureRequest request);

    @Mapping(target = "agendaId", source = "agenda.id")
    @Mapping(target = "speakerId", source = "speaker.id")
    @Mapping(target = "speakerName", source = "speaker.fullName")
    LectureResponse toResponse(Lecture lecture);
}
