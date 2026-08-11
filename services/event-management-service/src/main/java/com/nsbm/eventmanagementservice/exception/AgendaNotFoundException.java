package com.nsbm.eventmanagementservice.exception;

public class AgendaNotFoundException extends RuntimeException {
    public AgendaNotFoundException(Long id) {
        super("Agenda item not found with id: " + id);
    }
}
