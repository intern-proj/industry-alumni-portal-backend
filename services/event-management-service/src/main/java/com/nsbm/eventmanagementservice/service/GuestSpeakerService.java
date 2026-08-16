package com.nsbm.eventmanagementservice.service;

import com.nsbm.eventmanagementservice.dto.GuestSpeakerRequest;
import com.nsbm.eventmanagementservice.dto.GuestSpeakerResponse;

import java.util.List;

public interface GuestSpeakerService {
    GuestSpeakerResponse createSpeaker(GuestSpeakerRequest request);

    GuestSpeakerResponse getSpeakerById(Long id);

    List<GuestSpeakerResponse> getAllSpeakers();

    List<GuestSpeakerResponse> getSpeakersByOrganization(Long organizationId);

    GuestSpeakerResponse updateSpeaker(Long id, GuestSpeakerRequest request);

    void deleteSpeaker(Long id);
}
