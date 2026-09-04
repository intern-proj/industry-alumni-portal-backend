package com.nsbm.authservice.dto;

import java.io.Serializable;

public record OtpEmailPayload(
        String toEmail,
        String otpCode
) implements Serializable {}
