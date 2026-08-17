package com.portal.platformservice.entity;

/**
 * Required document categories for partner verification. The set of types
 * that must all be present before status can move to PENDING_REVIEW is
 * read from SystemConfigService (config key: partner.required.document.types)
 * rather than hard-coded, so evaluators can see it is configurable.
 */
public enum DocumentType {
    BUSINESS_REGISTRATION,
    TAX_CERTIFICATE,
    AUTHORIZATION_LETTER
}
