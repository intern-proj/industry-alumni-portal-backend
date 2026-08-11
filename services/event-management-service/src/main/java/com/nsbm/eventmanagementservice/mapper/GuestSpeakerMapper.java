package com.nsbm.eventmanagementservice.mapper;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;
import com.nsbm.eventmanagementservice.model.GuestSpeaker;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface GuestSpeakerMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GuestSpeaker toEntity(GuestSpeakerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(GuestSpeakerRequest request, @MappingTarget GuestSpeaker speaker);

    GuestSpeakerResponse toResponse(GuestSpeaker speaker);
}
