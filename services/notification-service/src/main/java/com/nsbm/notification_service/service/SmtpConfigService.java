package com.nsbm.notification_service.service;

import com.nsbm.notification_service.dto.SmtpConfigDTO;
import com.nsbm.notification_service.dto.SmtpTestRequestDTO;
import com.nsbm.notification_service.exception.EmailDeliveryException;
import com.nsbm.notification_service.model.SmtpConfiguration;
import com.nsbm.notification_service.repository.SmtpConfigurationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Properties;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmtpConfigService {

    private final SmtpConfigurationRepository smtpRepository;

    @Transactional(readOnly = true)
    public SmtpConfigDTO getActiveConfig() {
        return smtpRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Transactional
    public SmtpConfigDTO saveOrUpdateConfig(SmtpConfigDTO dto) {
        log.info("Updating active SMTP server configuration for host: {}:{}", dto.getHost(), dto.getPort());

        SmtpConfiguration config = smtpRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .orElse(new SmtpConfiguration());

        config.setHost(dto.getHost().trim());
        config.setPort(dto.getPort());
        config.setUsername(dto.getUsername() != null ? dto.getUsername().trim() : null);

        // If a new password is provided, update it; otherwise preserve existing password
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            config.setPassword(dto.getPassword().trim());
        }

        config.setSenderEmail(dto.getSenderEmail().trim());
        config.setSenderName(dto.getSenderName() != null ? dto.getSenderName().trim() : "NSBM Industry & Alumni Portal");
        config.setAuthEnabled(dto.getAuthEnabled() != null ? dto.getAuthEnabled() : true);
        config.setStarttlsEnabled(dto.getStarttlsEnabled() != null ? dto.getStarttlsEnabled() : true);
        config.setSslEnabled(dto.getSslEnabled() != null ? dto.getSslEnabled() : false);
        config.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        config.setUpdatedAt(LocalDateTime.now());

        SmtpConfiguration saved = smtpRepository.save(config);
        log.info("Successfully persisted SMTP configuration ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    /**
     * Dynamically builds a JavaMailSenderImpl instance using the provided SmtpConfiguration.
     */
    public JavaMailSenderImpl buildMailSender(SmtpConfiguration config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());

        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            sender.setUsername(config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            sender.setPassword(config.getPassword());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");

        boolean isAuth = config.getAuthEnabled() != null && config.getAuthEnabled();
        props.put("mail.smtp.auth", String.valueOf(isAuth));

        boolean isStarttls = config.getStarttlsEnabled() != null && config.getStarttlsEnabled();
        props.put("mail.smtp.starttls.enable", String.valueOf(isStarttls));

        boolean isSsl = config.getSslEnabled() != null && config.getSslEnabled();
        props.put("mail.smtp.ssl.enable", String.valueOf(isSsl));

        if (isSsl || config.getPort() == 465) {
            props.put("mail.smtp.socketFactory.port", String.valueOf(config.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");

        return sender;
    }

    /**
     * Tests sending a test email to verify credentials and SMTP connectivity.
     */
    public void testSmtpConnection(SmtpTestRequestDTO testDto) {
        log.info("Testing SMTP connection to recipient: {}", testDto.getRecipientEmail());

        SmtpConfiguration config;
        // If host/port are supplied in request, use them; otherwise use stored active config
        if (testDto.getHost() != null && !testDto.getHost().isBlank()) {
            config = SmtpConfiguration.builder()
                    .host(testDto.getHost().trim())
                    .port(testDto.getPort() != null ? testDto.getPort() : 587)
                    .username(testDto.getUsername())
                    .password(testDto.getPassword())
                    .senderEmail(testDto.getSenderEmail() != null ? testDto.getSenderEmail() : "notifications@nsbm.ac.lk")
                    .senderName(testDto.getSenderName() != null ? testDto.getSenderName() : "NSBM Portal Diagnostic")
                    .authEnabled(testDto.getAuthEnabled() != null ? testDto.getAuthEnabled() : true)
                    .starttlsEnabled(testDto.getStarttlsEnabled() != null ? testDto.getStarttlsEnabled() : true)
                    .sslEnabled(testDto.getSslEnabled() != null ? testDto.getSslEnabled() : false)
                    .build();

            // If password in testDto is blank, check if we can borrow the stored active password
            if ((config.getPassword() == null || config.getPassword().isBlank())) {
                smtpRepository.findFirstByIsActiveTrueOrderByIdDesc().ifPresent(active -> {
                    config.setPassword(active.getPassword());
                });
            }
        } else {
            config = smtpRepository.findFirstByIsActiveTrueOrderByIdDesc()
                    .orElseThrow(() -> new EmailDeliveryException(
                            testDto.getRecipientEmail(), "No active SMTP configuration found in database to test.", null));
        }

        JavaMailSender mailSender = buildMailSender(config);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = config.getSenderEmail();
            String fromDisplay = config.getSenderName() != null ? config.getSenderName() : "NSBM Portal";
            helper.setFrom(fromAddress, fromDisplay);
            helper.setTo(testDto.getRecipientEmail());
            helper.setSubject("✅ NSBM Industry & Alumni Portal - SMTP Diagnostic Verification");

            String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e2e8f0; rounded: 12px; background-color: #ffffff;'>"
                    + "<div style='text-align: center; margin-bottom: 20px;'>"
                    + "<h2 style='color: #059669; margin: 0;'>SMTP Server Diagnostic Test Passed</h2>"
                    + "<p style='color: #64748b; font-size: 14px;'>NSBM Green University Industry & Alumni Portal</p>"
                    + "</div>"
                    + "<p style='color: #334155; font-size: 14px;'>This is an automated test verifying that your SMTP server parameters are active and correctly configured in the portal database.</p>"
                    + "<div style='background-color: #f8fafc; padding: 16px; border-radius: 8px; font-size: 13px; color: #475569; margin: 16px 0;'>"
                    + "<p style='margin: 4px 0;'><strong>SMTP Host:</strong> " + config.getHost() + "</p>"
                    + "<p style='margin: 4px 0;'><strong>Port:</strong> " + config.getPort() + "</p>"
                    + "<p style='margin: 4px 0;'><strong>Authentication:</strong> " + (config.getAuthEnabled() ? "Enabled" : "Disabled") + "</p>"
                    + "<p style='margin: 4px 0;'><strong>Security:</strong> " + (config.getSslEnabled() ? "SSL (465)" : (config.getStarttlsEnabled() ? "STARTTLS (587)" : "Plain")) + "</p>"
                    + "<p style='margin: 4px 0;'><strong>Sender Email:</strong> " + config.getSenderEmail() + "</p>"
                    + "<p style='margin: 4px 0;'><strong>Timestamp:</strong> " + LocalDateTime.now() + "</p>"
                    + "</div>"
                    + "<p style='color: #64748b; font-size: 12px; text-align: center;'>All notification queues (OTP, Invitations, Vacancy Updates, Announcements) will utilize these parameters.</p>"
                    + "</div>";

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Diagnostic SMTP test email successfully delivered to {}", testDto.getRecipientEmail());
        } catch (Exception ex) {
            log.error("SMTP diagnostic test failed for recipient {}: {}", testDto.getRecipientEmail(), ex.getMessage());
            throw new EmailDeliveryException(testDto.getRecipientEmail(), "SMTP Test Connection Failed: " + ex.getMessage(), ex);
        }
    }

    private SmtpConfigDTO mapToDTO(SmtpConfiguration entity) {
        return SmtpConfigDTO.builder()
                .id(entity.getId())
                .host(entity.getHost())
                .port(entity.getPort())
                .username(entity.getUsername())
                .password(entity.getPassword() != null && !entity.getPassword().isBlank() ? "••••••••" : "")
                .isPasswordSet(entity.getPassword() != null && !entity.getPassword().isBlank())
                .senderEmail(entity.getSenderEmail())
                .senderName(entity.getSenderName())
                .authEnabled(entity.getAuthEnabled())
                .starttlsEnabled(entity.getStarttlsEnabled())
                .sslEnabled(entity.getSslEnabled())
                .isActive(entity.getIsActive())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
