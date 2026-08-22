package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaRequest;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaResponse;
import com.nsbm.eventmanagementservice.exception.CertificateEligibilityCriteriaNotFoundException;
import com.nsbm.eventmanagementservice.exception.EventNotFoundException;
import com.nsbm.eventmanagementservice.mapper.CertificateEligibilityCriteriaMapper;
import com.nsbm.eventmanagementservice.model.CertificateEligibilityCriteria;
import com.nsbm.eventmanagementservice.model.Event;
import com.nsbm.eventmanagementservice.repository.CertificateEligibilityCriteriaRepository;
import com.nsbm.eventmanagementservice.repository.EventRepository;
import com.nsbm.eventmanagementservice.service.CertificateEligibilityCriteriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CertificateEligibilityCriteriaServiceImpl implements CertificateEligibilityCriteriaService {
    private final CertificateEligibilityCriteriaRepository criteriaRepository;
    private final EventRepository eventRepository;
    private final CertificateEligibilityCriteriaMapper criteriaMapper;

    @Override
    public CertificateEligibilityCriteriaResponse createOrUpdateCriteria(Long eventId, CertificateEligibilityCriteriaRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        CertificateEligibilityCriteria criteria = criteriaRepository.findByEventId(eventId)
                .orElseGet(() -> {
                    CertificateEligibilityCriteria newCriteria = criteriaMapper.toEntity(request);
                    newCriteria.setEvent(event);
                    return newCriteria;
                });

        criteriaMapper.updateEntityFromRequest(request, criteria);

        CertificateEligibilityCriteria saved = criteriaRepository.save(criteria);
        return criteriaMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateEligibilityCriteriaResponse getCriteriaByEventId(Long eventId) {
        CertificateEligibilityCriteria criteria = criteriaRepository.findByEventId(eventId)
                .orElseThrow(() -> new CertificateEligibilityCriteriaNotFoundException(eventId));
        return criteriaMapper.toResponse(criteria);
    }

    @Override
    public void deleteCriteria(Long eventId) {
        CertificateEligibilityCriteria criteria = criteriaRepository.findByEventId(eventId)
                .orElseThrow(() -> new CertificateEligibilityCriteriaNotFoundException(eventId));
        criteriaRepository.delete(criteria);
    }

}
