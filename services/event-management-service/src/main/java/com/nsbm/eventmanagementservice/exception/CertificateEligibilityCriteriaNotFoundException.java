package com.nsbm.eventmanagementservice.exception;

public class CertificateEligibilityCriteriaNotFoundException extends RuntimeException {
    public CertificateEligibilityCriteriaNotFoundException(Long eventId) {
        super("Certificate eligibility criteria not found for event id: " + eventId);
    }
}
