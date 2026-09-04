package com.nsbm.notification_service.service.core;

import com.nsbm.notification_service.exception.EmailDeliveryException;
import com.nsbm.notification_service.exception.NotificationValidationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailDeliveryService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.mock-sending:false}")
    private boolean mockSending;

    private final JavaMailSender javaMailSender;
    private final com.nsbm.notification_service.repository.SmtpConfigurationRepository smtpRepository;
    private final com.nsbm.notification_service.service.SmtpConfigService smtpConfigService;

    private JavaMailSender resolveMailSender() {
        return smtpRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .filter(cfg -> cfg.getHost() != null && !cfg.getHost().isBlank())
                .<JavaMailSender>map(smtpConfigService::buildMailSender)
                .orElse(javaMailSender);
    }

    private String resolveFromEmail() {
        return smtpRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .filter(cfg -> cfg.getSenderEmail() != null && !cfg.getSenderEmail().isBlank())
                .map(com.nsbm.notification_service.model.SmtpConfiguration::getSenderEmail)
                .orElse(fromEmail);
    }

    private String resolveFromName() {
        return smtpRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .filter(cfg -> cfg.getSenderName() != null && !cfg.getSenderName().isBlank())
                .map(com.nsbm.notification_service.model.SmtpConfiguration::getSenderName)
                .orElse("NSBM Industry & Alumni Portal");
    }

    /**
     * Sends a plain-text email. Used by OTP and simple reminder flows.
     */
    public void sendEmail(String to, String subject, String body) {
        validateEmailParams(to, subject);

        if (mockSending) {
            log.info("\n========== MOCK EMAIL SENT ==========\nTo: {}\nSubject: {}\n\nBody:\n{}\n=======================================\n", to, subject, body);
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            mail.setFrom(resolveFromEmail());
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);

            resolveMailSender().send(mail);

            log.info("Email sent successfully to - {}", to);

        } catch (MailException ex) {
            log.error("Failed sending plain-text email to {}: {}", to, ex.getMessage());
            throw new EmailDeliveryException(to, "Failed to deliver email to " + to, ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending email to {}: {}", to, ex.getMessage());
            throw new EmailDeliveryException(to, "Unexpected email delivery error for " + to, ex);
        }
    }

    /**
     * Sends an HTML email. Used by Announcements, Invitations and rich Update notifications.
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        validateEmailParams(to, subject);

        if (mockSending) {
            log.info("\n========== MOCK HTML EMAIL SENT ==========\nTo: {}\nSubject: {}\n\nHTML Body:\n{}\n============================================\n", to, subject, htmlBody);
            return;
        }

        try {
            JavaMailSender activeSender = resolveMailSender();
            MimeMessage message = activeSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(resolveFromEmail(), resolveFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            activeSender.send(message);

            log.info("HTML Email sent successfully to - {}", to);

        } catch (MessagingException | MailException ex) {
            log.error("Failed sending HTML email to {}: {}", to, ex.getMessage());
            throw new EmailDeliveryException(to, "Failed to deliver HTML email to " + to, ex);
        } catch (Exception ex) {
            log.error("Unexpected error sending HTML email to {}: {}", to, ex.getMessage());
            throw new EmailDeliveryException(to, "Unexpected HTML email delivery error for " + to, ex);
        }
    }

    private void validateEmailParams(String to, String subject) {
        if (to == null || to.isBlank()) {
            throw new NotificationValidationException("Recipient email address cannot be null or blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new NotificationValidationException("Email subject cannot be null or blank");
        }
    }
}


