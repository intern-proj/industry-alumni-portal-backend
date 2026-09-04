package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.AgendaRequest;
import com.nsbm.eventmanagementservice.dto.AgendaResponse;
import com.nsbm.eventmanagementservice.exception.AgendaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.exception.GuestSpeakerNotFoundException;
import com.nsbm.eventmanagementservice.mapper.AgendaMapper;
import com.nsbm.eventmanagementservice.model.Agenda;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.repository.AgendaRepository;
import com.nsbm.eventmanagementservice.repository.EventRepository;
import com.nsbm.eventmanagementservice.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AgendaServiceImpl implements AgendaService {
    private final AgendaRepository agendaRepository;
    private final EventRepository eventRepository;
    private final AgendaMapper agendaMapper;

    @Override
    public AgendaResponse createAgendaItem(AgendaRequest request) {
        Agenda agenda = agendaMapper.toEntity(request);

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EventNotFoundException(request.getEventId()));
        agenda.setEvent(event);



        Agenda saved = agendaRepository.save(agenda);
        return agendaMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgendaResponse getAgendaItemById(Long id) {
        Agenda agenda = findAgendaOrThrow(id);
        return agendaMapper.toResponse(agenda);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgendaResponse> getAgendaByEventId(Long eventId) {
        return agendaRepository.findByEventIdOrderBySequenceOrderAsc(eventId).stream()
                .map(agendaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgendaResponse> getAgendaBySpeakerId(Long speakerId) {
        // Now speakers are tied to lectures, so this would need a custom query or join.
        // For now, return empty list or implement proper repository method.
        return List.of();
    }

    @Override
    public AgendaResponse updateAgendaItem(Long id, AgendaRequest request) {
        Agenda agenda = findAgendaOrThrow(id);
        agendaMapper.updateEntityFromRequest(request, agenda);

        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new EventNotFoundException(request.getEventId()));
            agenda.setEvent(event);
        }



        Agenda saved = agendaRepository.save(agenda);
        return agendaMapper.toResponse(saved);
    }

    @Override
    public void deleteAgendaItem(Long id) {
        Agenda agenda = findAgendaOrThrow(id);
        agendaRepository.delete(agenda);
    }

    private Agenda findAgendaOrThrow(Long id) {
        return agendaRepository.findById(id)
                .orElseThrow(() -> new AgendaNotFoundException(id));
    }

}
