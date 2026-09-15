package com.portal.certificateservice.service;

import com.portal.certificateservice.dto.CreateTemplateRequestDto;
import com.portal.certificateservice.dto.TemplateResponseDto;
import com.portal.certificateservice.dto.UpdateTemplateRequestDto;
import com.portal.certificateservice.entity.CertificateTemplate;
import com.portal.certificateservice.exception.CertificateException;
import com.portal.certificateservice.exception.ResourceNotFoundException;
import com.portal.certificateservice.repository.CertificateTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CertificateTemplateService {

    private final CertificateTemplateRepository templateRepository;

    public CertificateTemplateService(CertificateTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public TemplateResponseDto createTemplate(CreateTemplateRequestDto request) {
        if (templateRepository.existsByTemplateName(request.getTemplateName())) {
            throw new CertificateException("Template with name '" + request.getTemplateName() + "' already exists");
        }

        CertificateTemplate template = new CertificateTemplate(
                request.getTemplateName(),
                request.getTemplateFilePath(),
                request.getFieldsConfig(),
                request.getIsActive()
        );

        CertificateTemplate saved = templateRepository.save(template);
        return mapToResponseDto(saved);
    }

    public List<TemplateResponseDto> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<TemplateResponseDto> getActiveTemplates() {
        return templateRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public TemplateResponseDto getTemplateById(UUID id) {
        CertificateTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found with ID: " + id));
        return mapToResponseDto(template);
    }

    public TemplateResponseDto updateTemplate(UUID id, UpdateTemplateRequestDto request) {
        CertificateTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found with ID: " + id));

        if (request.getTemplateName() != null && !request.getTemplateName().isBlank()) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getTemplateFilePath() != null) {
            template.setTemplateFilePath(request.getTemplateFilePath());
        }
        if (request.getFieldsConfig() != null) {
            template.setFieldsConfig(request.getFieldsConfig());
        }
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }

        CertificateTemplate updated = templateRepository.save(template);
        return mapToResponseDto(updated);
    }

    public void deleteTemplate(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Certificate template not found with ID: " + id);
        }
        templateRepository.deleteById(id);
    }

    public CertificateTemplate getEntityById(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate template not found with ID: " + id));
    }

    private TemplateResponseDto mapToResponseDto(CertificateTemplate template) {
        return new TemplateResponseDto(
                template.getId(),
                template.getTemplateName(),
                template.getTemplateFilePath(),
                template.getFieldsConfig(),
                template.getIsActive(),
                template.getCreatedAt()
        );
    }
}
