package com.nsbm.eventmanagementservice.service.impl;

import com.nsbm.eventmanagementservice.dto.VenueRequest;
import com.nsbm.eventmanagementservice.dto.VenueResponse;
import com.nsbm.eventmanagementservice.exception.VenueNotFoundException;
import com.nsbm.eventmanagementservice.mapper.VenueMapper;
import com.nsbm.eventmanagementservice.model.Venue;
import com.nsbm.eventmanagementservice.repository.VenueRepository;
import com.nsbm.eventmanagementservice.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VenueServiceImpl implements VenueService {
    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    @Override
    public VenueResponse createVenue(VenueRequest request) {
        Venue venue = venueMapper.toEntity(request);
        Venue saved = venueRepository.save(venue);
        return venueMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = findVenueOrThrow(id);
        return venueMapper.toResponse(venue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(venueMapper::toResponse)
                .toList();
    }

    @Override
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        Venue venue = findVenueOrThrow(id);
        venueMapper.updateEntityFromRequest(request, venue);
        Venue saved = venueRepository.save(venue);
        return venueMapper.toResponse(saved);
    }

    @Override
    public void deleteVenue(Long id) {
        Venue venue = findVenueOrThrow(id);
        venueRepository.delete(venue);
    }

    private Venue findVenueOrThrow(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException(id));
    }
}
