package com.portal.platformservice.event;

import com.portal.platformservice.entity.VerificationStatus;

import java.util.UUID;

public record PartnerVerificationDecidedEvent(
        UUID verificationId,
        UUID partnerUserId,
        VerificationStatus outcome
) {
}
