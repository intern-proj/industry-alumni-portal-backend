package com.nsbm.eventmanagementservice.exception;

public class DuplicateSpeakerEmailException extends RuntimeException {
    public DuplicateSpeakerEmailException(String message) {
        super(message);
    }
}
