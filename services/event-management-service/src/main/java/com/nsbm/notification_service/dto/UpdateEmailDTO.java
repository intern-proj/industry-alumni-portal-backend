package com.nsbm.notification_service.dto;

import java.io.Serializable;

public record UpdateEmailDTO(
        String toEmail,
        String recipientName,
        String updateType,
        String updateBody,
        String actionLink
) implements Serializable {}
