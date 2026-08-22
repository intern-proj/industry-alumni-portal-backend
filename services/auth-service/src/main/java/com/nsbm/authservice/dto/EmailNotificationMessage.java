package com.nsbm.authservice.dto;

import java.io.Serializable;

public record EmailNotificationMessage(
        String recipientEmail,
        String subject,
        String body,
        String eventType
) implements Serializable {}
