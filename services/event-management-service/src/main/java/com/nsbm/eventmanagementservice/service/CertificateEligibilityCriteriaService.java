package com.nsbm.eventmanagementservice.service;

import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaRequest;
import com.nsbm.eventmanagementservice.dto.CertificateEligibilityCriteriaResponse;

public interface CertificateEligibilityCriteriaService {
    CertificateEligibilityCriteriaResponse createOrUpdateCriteria(Long eventId, CertificateEligibilityCriteriaRequest request);

    CertificateEligibilityCriteriaResponse getCriteriaByEventId(Long eventId);

    void deleteCriteria(Long eventId);
}
