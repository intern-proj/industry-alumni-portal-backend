package com.portal.platformservice.entity;

/**
 * Review-workflow status for a vacancy approval record, owned by
 * Platform Management. This is distinct from the vacancy's own lifecycle
 * status in Vacancy Service (DRAFT/PENDING/PUBLISHED/CLOSED) — that
 * remains Vacancy Service's source of truth. This enum tracks only the
 * state of the review itself.
 */
public enum VacancyApprovalStatus {
    PENDING_REVIEW,
    UNDER_REVIEW,
    APPROVED,
    REJECTED
}
