package com.portal.certificateservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTemplateRequestDto {

    @NotBlank(message = "Template name is required")
    private String templateName;

    private String templateFilePath;
    private String fieldsConfig;
    private Boolean isActive = true;

    public CreateTemplateRequestDto() {
    }

    public CreateTemplateRequestDto(String templateName, String templateFilePath, String fieldsConfig, Boolean isActive) {
        this.templateName = templateName;
        this.templateFilePath = templateFilePath;
        this.fieldsConfig = fieldsConfig;
        this.isActive = isActive != null ? isActive : true;
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
}
