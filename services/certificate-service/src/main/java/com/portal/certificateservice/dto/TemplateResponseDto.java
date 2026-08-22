package com.portal.certificateservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class TemplateResponseDto {

    private UUID id;
    private String templateName;
    private String templateFilePath;
    private String fieldsConfig;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public TemplateResponseDto() {
    }

    public TemplateResponseDto(UUID id, String templateName, String templateFilePath, String fieldsConfig, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.templateName = templateName;
        this.templateFilePath = templateFilePath;
        this.fieldsConfig = fieldsConfig;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateFilePath() {
        return templateFilePath;
    }

    public void setTemplateFilePath(String templateFilePath) {
        this.templateFilePath = templateFilePath;
    }

    public String getFieldsConfig() {
        return fieldsConfig;
    }

    public void setFieldsConfig(String fieldsConfig) {
        this.fieldsConfig = fieldsConfig;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
