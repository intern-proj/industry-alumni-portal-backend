package com.nsbm.notification_service.exception;

public class EmailDeliveryException extends NotificationException {

    private final String recipient;

    public EmailDeliveryException(String recipient, String message) {
        super(message);
        this.recipient = recipient;
    }

    public EmailDeliveryException(String recipient, String message, Throwable cause) {
        super(message, cause);
        this.recipient = recipient;
    }

    public String getRecipient() {
        return recipient;
    }
}
