package com.nsbm.notification_service.exception;

public class NotificationValidationException extends NotificationException {

    public NotificationValidationException(String message) {
        super(message);
    }

    public NotificationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
