package com.portal.platformservice.entity;

/**
 * Discriminates which workflow an ApprovalStatusHistory row belongs to.
 * Used together with approvalId (a logical, non-FK reference) since one
 * history table is shared across both partner verification and vacancy
 * approval, and a real foreign key cannot point at two different tables.
 */
public enum ApprovalType {
    PARTNER_VERIFICATION,
    VACANCY_APPROVAL
}
