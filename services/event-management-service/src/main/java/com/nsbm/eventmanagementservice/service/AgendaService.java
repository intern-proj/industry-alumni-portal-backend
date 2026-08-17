package com.nsbm.eventmanagementservice.service;

import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;

import java.util.List;

public interface AgendaService {
    AgendaResponse createAgendaItem(AgendaRequest request);

    AgendaResponse getAgendaItemById(Long id);

    List<AgendaResponse> getAgendaByEventId(Long eventId);

    List<AgendaResponse> getAgendaBySpeakerId(Long speakerId);

    AgendaResponse updateAgendaItem(Long id, AgendaRequest request);

    void deleteAgendaItem(Long id);
}
