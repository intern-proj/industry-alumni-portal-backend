package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationTemplateDTO {

    private Long id;

    @NotBlank(message = "Template code must not be blank")
    private String templateCode;

    @NotBlank(message = "Template name must not be blank")
    private String name;

    @NotBlank(message = "Template subject must not be blank")
    private String subject;

    @NotBlank(message = "Template body must not be blank")
    private String body;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
