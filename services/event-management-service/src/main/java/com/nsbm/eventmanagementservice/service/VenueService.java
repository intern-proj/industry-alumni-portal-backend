package com.nsbm.eventmanagementservice.service;

import com.nsbm.eventmanagementservice.dto.VenueRequest;
import com.nsbm.eventmanagementservice.dto.VenueResponse;
import com.nsbm.eventmanagementservice.exception.VenueNotFoundException;
import com.nsbm.eventmanagementservice.mapper.VenueMapper;
import com.nsbm.eventmanagementservice.model.Venue;
import com.nsbm.eventmanagementservice.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    @Transactional
    public VenueResponse createVenue(VenueRequest request) {
        log.info("Creating venue: {}", request.getName());
        Venue venue = venueMapper.toEntity(request);
        Venue saved = venueRepository.save(venue);
        return venueMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException(id));
        return venueMapper.toResponse(venue);
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(venueMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        log.info("Updating venue with ID: {}", id);
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new VenueNotFoundException(id));

        venueMapper.updateEntityFromRequest(request, venue);
        Venue updated = venueRepository.save(venue);
        return venueMapper.toResponse(updated);
    }

    @Transactional
    public void deleteVenue(Long id) {
        log.info("Deleting venue with ID: {}", id);
        if (!venueRepository.existsById(id)) {
            throw new VenueNotFoundException(id);
        }
        venueRepository.deleteById(id);
    }
}
