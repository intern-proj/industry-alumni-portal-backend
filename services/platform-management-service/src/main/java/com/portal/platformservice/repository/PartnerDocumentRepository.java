package com.portal.platformservice.repository;

import com.portal.platformservice.entity.PartnerDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnerDocumentRepository extends JpaRepository<PartnerDocument, UUID> {

    List<PartnerDocument> findByVerification_Id(UUID verificationId);
}
