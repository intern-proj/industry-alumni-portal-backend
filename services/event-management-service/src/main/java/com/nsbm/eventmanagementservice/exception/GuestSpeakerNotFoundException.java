package com.nsbm.eventmanagementservice.exception;

public class GuestSpeakerNotFoundException extends RuntimeException {
    public GuestSpeakerNotFoundException(Long id) {
        super("Guest speaker not found with id: " + id);
    }
}
