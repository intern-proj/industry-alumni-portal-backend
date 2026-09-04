package com.nsbm.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmtpTestRequestDTO {

    @NotBlank(message = "Recipient test email must not be blank")
    @Email(message = "Invalid recipient test email format")
    private String recipientEmail;

    // Optional override fields to test without saving first
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String senderEmail;
    private String senderName;
    private Boolean authEnabled;
    private Boolean starttlsEnabled;
    private Boolean sslEnabled;
}
