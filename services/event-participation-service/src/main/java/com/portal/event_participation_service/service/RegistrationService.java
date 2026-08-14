package com.portal.event_participation_service.service;

import com.portal.event_participation_service.dto.RegistrationRequest;
import com.portal.event_participation_service.dto.RegistrationResponse;
import com.portal.event_participation_service.entity.Registration;
import com.portal.event_participation_service.exception.ResourceNotFoundException;
import com.portal.event_participation_service.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;

    public RegistrationResponse register(RegistrationRequest request) {
        Registration registration = Registration.builder()
                .eventId(request.eventId())
                .studentId(request.studentId())
                .status(Registration.RegistrationStatus.PENDING)
                .build();
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    public RegistrationResponse getById(UUID registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registration with ID " + registrationId + " was not found."));
        return RegistrationResponse.from(registration);
    }

    public List<RegistrationResponse> getByEvent(UUID eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .map(RegistrationResponse::from)
                .toList();
    }

    public RegistrationResponse updateStatus(UUID registrationId, Registration.RegistrationStatus newStatus) {
    Registration registration = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Registration with ID " + registrationId + " was not found."));
    registration.setStatus(newStatus);
    return RegistrationResponse.from(registrationRepository.save(registration));
    }

    public void delete(UUID registrationId) {
    if (!registrationRepository.existsById(registrationId)) {
        throw new ResourceNotFoundException("Registration with ID " + registrationId + " was not found.");
    }
    registrationRepository.deleteById(registrationId);
}

    public List<RegistrationResponse> getAll() {
    return registrationRepository.findAll().stream()
            .map(RegistrationResponse::from)
            .toList();
}

    
}