package com.portal.platformservice.entity;

/**
 * Lifecycle states for a partner verification record. Valid transitions
 * are enforced by ApprovalStateMachine, not by this enum itself.
 *
 *   PENDING_DOCUMENTS --> PENDING_REVIEW --> UNDER_REVIEW --> APPROVED
 *                                                          \-> REJECTED
 *                                                          \-> MORE_INFO_REQUIRED --> PENDING_DOCUMENTS
 */
public enum VerificationStatus {
    PENDING_DOCUMENTS,
    PENDING_REVIEW,
    UNDER_REVIEW,
    MORE_INFO_REQUIRED,
    APPROVED,
    REJECTED
}
