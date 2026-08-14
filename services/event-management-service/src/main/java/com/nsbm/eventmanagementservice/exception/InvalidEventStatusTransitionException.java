package com.nsbm.eventmanagementservice.exception;

import com.nsbm.eventmanagementservice.model.EventStatus;

public class InvalidEventStatusTransitionException extends RuntimeException {
    public InvalidEventStatusTransitionException(EventStatus currentStatus, EventStatus targetStatus) {
        super("Cannot transition event from status " + currentStatus + " to " + targetStatus);
    }
}
