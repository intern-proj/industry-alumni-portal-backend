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
        var existing = registrationRepository.findByEventIdAndStudentId(request.eventId(), request.studentId());
        if (existing.isPresent()) {
            return RegistrationResponse.from(existing.get());
        }

        Registration registration = Registration.builder()
                .eventId(request.eventId())
                .studentId(request.studentId())
                .eventTitle(request.eventTitle())
                .venueName(request.venueName())
                .status(Registration.RegistrationStatus.REGISTERED)
                .build();
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    public RegistrationResponse getById(UUID registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registration with ID " + registrationId + " was not found."));
        return RegistrationResponse.from(registration);
    }

    public List<RegistrationResponse> getByEvent(String eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .map(RegistrationResponse::from)
                .toList();
    }

    public List<RegistrationResponse> getByStudent(String studentId) {
        return registrationRepository.findByStudentId(studentId).stream()
                .map(RegistrationResponse::from)
                .toList();
    }

    public List<RegistrationResponse> getByEventAndStudent(String eventId, String studentId) {
        return registrationRepository.findByEventIdAndStudentId(eventId, studentId).stream()
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