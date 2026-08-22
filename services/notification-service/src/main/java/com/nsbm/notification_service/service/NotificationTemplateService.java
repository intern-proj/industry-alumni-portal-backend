package com.nsbm.notification_service.service;

import com.nsbm.notification_service.dto.NotificationTemplateDTO;
import com.nsbm.notification_service.exception.DuplicateTemplateException;
import com.nsbm.notification_service.exception.TemplateNotFoundException;
import com.nsbm.notification_service.model.NotificationTemplate;
import com.nsbm.notification_service.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<NotificationTemplateDTO> getAllTemplates() {
        log.info("Fetching all notification email templates");
        return templateRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateDTO getTemplateById(Long id) {
        log.info("Fetching notification template with ID: {}", id);
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Notification template not found with ID: " + id));
        return mapToDTO(template);
    }

    @Transactional(readOnly = true)
    public NotificationTemplateDTO getTemplateByCode(String templateCode) {
        log.info("Fetching notification template with code: {}", templateCode);
        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new TemplateNotFoundException("Notification template not found with code: " + templateCode));
        return mapToDTO(template);
    }

    @Transactional
    public NotificationTemplateDTO createTemplate(NotificationTemplateDTO dto) {
        log.info("Saving new notification template with code: {}", dto.getTemplateCode());
        if (templateRepository.existsByTemplateCode(dto.getTemplateCode())) {
            throw new DuplicateTemplateException("Template already exists with code: " + dto.getTemplateCode());
        }

        NotificationTemplate entity = mapToEntity(dto);
        NotificationTemplate saved = templateRepository.save(entity);
        log.info("Successfully created notification template with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    @Transactional
    public NotificationTemplateDTO updateTemplate(Long id, NotificationTemplateDTO dto) {
        log.info("Updating notification template with ID: {}", id);
        NotificationTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Notification template not found with ID: " + id));

        // If templateCode is being changed, ensure the new code is unique
        if (!existing.getTemplateCode().equalsIgnoreCase(dto.getTemplateCode())
                && templateRepository.existsByTemplateCode(dto.getTemplateCode())) {
            throw new DuplicateTemplateException("Template code '" + dto.getTemplateCode() + "' is already in use");
        }

        existing.setTemplateCode(dto.getTemplateCode());
        existing.setName(dto.getName());
        existing.setSubject(dto.getSubject());
        existing.setBody(dto.getBody());
        existing.setDescription(dto.getDescription());

        NotificationTemplate updated = templateRepository.save(existing);
        log.info("Successfully updated notification template ID: {}", id);
        return mapToDTO(updated);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        log.info("Deleting notification template with ID: {}", id);
        if (!templateRepository.existsById(id)) {
            throw new TemplateNotFoundException("Cannot delete: Notification template not found with ID: " + id);
        }
        templateRepository.deleteById(id);
        log.info("Successfully deleted notification template with ID: {}", id);
    }

    private NotificationTemplateDTO mapToDTO(NotificationTemplate entity) {
        return NotificationTemplateDTO.builder()
                .id(entity.getId())
                .templateCode(entity.getTemplateCode())
                .name(entity.getName())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NotificationTemplate mapToEntity(NotificationTemplateDTO dto) {
        return NotificationTemplate.builder()
                .id(dto.getId())
                .templateCode(dto.getTemplateCode())
                .name(dto.getName())
                .subject(dto.getSubject())
                .body(dto.getBody())
                .description(dto.getDescription())
                .build();
    }
}

