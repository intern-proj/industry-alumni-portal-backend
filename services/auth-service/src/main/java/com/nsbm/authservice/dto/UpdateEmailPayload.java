package com.nsbm.authservice.dto;

import java.io.Serializable;

public record UpdateEmailPayload(
        String toEmail,
        String recipientName,
        String updateType,
        String updateBody,
        String actionLink
) implements Serializable {}
