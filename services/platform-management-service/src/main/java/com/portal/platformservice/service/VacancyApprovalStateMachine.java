package com.portal.platformservice.service;

import com.portal.platformservice.entity.VacancyApprovalStatus;
import com.portal.platformservice.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class VacancyApprovalStateMachine {

    private static final Map<VacancyApprovalStatus, Set<VacancyApprovalStatus>> TRANSITIONS =
            new EnumMap<>(VacancyApprovalStatus.class);

    static {
        TRANSITIONS.put(VacancyApprovalStatus.PENDING_REVIEW, EnumSet.of(VacancyApprovalStatus.UNDER_REVIEW));
        TRANSITIONS.put(VacancyApprovalStatus.UNDER_REVIEW, EnumSet.of(
                VacancyApprovalStatus.APPROVED, VacancyApprovalStatus.REJECTED));
        TRANSITIONS.put(VacancyApprovalStatus.APPROVED, EnumSet.noneOf(VacancyApprovalStatus.class));
        TRANSITIONS.put(VacancyApprovalStatus.REJECTED, EnumSet.noneOf(VacancyApprovalStatus.class));
    }

    public void validateTransition(VacancyApprovalStatus from, VacancyApprovalStatus to) {
        Set<VacancyApprovalStatus> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition vacancy approval from " + from + " to " + to);
        }
    }
}
