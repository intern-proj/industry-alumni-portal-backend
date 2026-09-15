package com.portal.platformservice.service;

import com.portal.platformservice.entity.VerificationStatus;
import com.portal.platformservice.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PartnerVerificationStateMachine {

    private static final Map<VerificationStatus, Set<VerificationStatus>> TRANSITIONS =
            new EnumMap<>(VerificationStatus.class);

    static {
        TRANSITIONS.put(VerificationStatus.PENDING_DOCUMENTS, EnumSet.of(VerificationStatus.PENDING_REVIEW));
        TRANSITIONS.put(VerificationStatus.PENDING_REVIEW, EnumSet.of(VerificationStatus.UNDER_REVIEW));
        TRANSITIONS.put(VerificationStatus.UNDER_REVIEW, EnumSet.of(
                VerificationStatus.APPROVED, VerificationStatus.REJECTED, VerificationStatus.MORE_INFO_REQUIRED));
        TRANSITIONS.put(VerificationStatus.MORE_INFO_REQUIRED, EnumSet.of(VerificationStatus.PENDING_DOCUMENTS));
        TRANSITIONS.put(VerificationStatus.APPROVED, EnumSet.noneOf(VerificationStatus.class));
        TRANSITIONS.put(VerificationStatus.REJECTED, EnumSet.of(VerificationStatus.PENDING_DOCUMENTS));
    }

    public void validateTransition(VerificationStatus from, VerificationStatus to) {
        Set<VerificationStatus> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition partner verification from " + from + " to " + to);
        }
    }
}
