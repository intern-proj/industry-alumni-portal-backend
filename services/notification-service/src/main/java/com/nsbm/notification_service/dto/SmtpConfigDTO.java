package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmtpConfigDTO {

    private Long id;

    @NotBlank(message = "SMTP host must not be blank")
    private String host;

    @NotNull(message = "SMTP port must not be null")
    private Integer port;

    private String username;

    private String password;

    private Boolean isPasswordSet;

    @NotBlank(message = "Sender email address must not be blank")
    private String senderEmail;

    private String senderName;

    private Boolean authEnabled;

    private Boolean starttlsEnabled;

    private Boolean sslEnabled;

    private Boolean isActive;

    private LocalDateTime updatedAt;
}
