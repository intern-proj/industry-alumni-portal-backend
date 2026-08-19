package com.portal.certificateservice.repository;

import com.portal.certificateservice.entity.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {

    List<CertificateTemplate> findByIsActiveTrue();

    boolean existsByTemplateName(String templateName);
}
