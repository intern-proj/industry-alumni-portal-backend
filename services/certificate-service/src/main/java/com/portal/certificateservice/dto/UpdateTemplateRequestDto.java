package com.portal.certificateservice.dto;

public class UpdateTemplateRequestDto {

    private String templateName;
    private String templateFilePath;
    private String fieldsConfig;
    private Boolean isActive;

    public UpdateTemplateRequestDto() {
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
