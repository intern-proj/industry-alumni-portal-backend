package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;
import com.nsbm.eventmanagementservice.exception.GuestSpeakerNotFoundException;
import com.nsbm.eventmanagementservice.mapper.GuestSpeakerMapper;
import com.nsbm.eventmanagementservice.model.GuestSpeaker;
import com.nsbm.eventmanagementservice.repository.GuestSpeakerRepository;
import com.nsbm.eventmanagementservice.service.GuestSpeakerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GuestSpeakerServiceImpl implements GuestSpeakerService {
    private final GuestSpeakerRepository guestSpeakerRepository;
    private final GuestSpeakerMapper guestSpeakerMapper;

    @Override
    public GuestSpeakerResponse createSpeaker(GuestSpeakerRequest request) {
        GuestSpeaker speaker = guestSpeakerMapper.toEntity(request);
        GuestSpeaker saved = guestSpeakerRepository.save(speaker);
        return guestSpeakerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GuestSpeakerResponse getSpeakerById(Long id) {
        GuestSpeaker speaker = findSpeakerOrThrow(id);
        return guestSpeakerMapper.toResponse(speaker);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestSpeakerResponse> getAllSpeakers() {
        return guestSpeakerRepository.findAll().stream()
                .map(guestSpeakerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestSpeakerResponse> getSpeakersByOrganization(Long organizationId) {
        return guestSpeakerRepository.findByOrganizationId(organizationId).stream()
                .map(guestSpeakerMapper::toResponse)
                .toList();
    }

    @Override
    public GuestSpeakerResponse updateSpeaker(Long id, GuestSpeakerRequest request) {
        GuestSpeaker speaker = findSpeakerOrThrow(id);
        guestSpeakerMapper.updateEntityFromRequest(request, speaker);
        GuestSpeaker saved = guestSpeakerRepository.save(speaker);
        return guestSpeakerMapper.toResponse(saved);
    }

    @Override
    public void deleteSpeaker(Long id) {
        GuestSpeaker speaker = findSpeakerOrThrow(id);
        guestSpeakerRepository.delete(speaker);
    }

    private GuestSpeaker findSpeakerOrThrow(Long id) {
        return guestSpeakerRepository.findById(id)
                .orElseThrow(() -> new GuestSpeakerNotFoundException(id));
    }
}
