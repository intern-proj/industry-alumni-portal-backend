package com.nsbm.eventmanagementservice.service;

import com.nsbm.eventmanagementservice.dto.VenueRequest;
import com.nsbm.eventmanagementservice.dto.VenueResponse;

import java.util.List;

public interface VenueService {
    VenueResponse createVenue(VenueRequest request);

    VenueResponse getVenueById(Long id);

    List<VenueResponse> getAllVenues();

    VenueResponse updateVenue(Long id, VenueRequest request);

    void deleteVenue(Long id);
}
