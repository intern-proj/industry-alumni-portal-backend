package com.nsbm.eventmanagementservice.mapper;

import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaRequest;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaResponse;
import com.nsbm.eventmanagementservice.model.CertificateEligibilityCriteria;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CertificateEligibilityCriteriaMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CertificateEligibilityCriteria toEntity(CertificateEligibilityCriteriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CertificateEligibilityCriteriaRequest request, @MappingTarget CertificateEligibilityCriteria criteria);

    @Mapping(target = "eventId", source = "event.id")
    CertificateEligibilityCriteriaResponse toResponse(CertificateEligibilityCriteria criteria);
}
