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

    private final JavaMailSender javaMailSender;

    /**
     * Sends a plain-text email. Used by OTP and simple reminder flows.
     */
    public void sendEmail(String to, String subject, String body) {
        validateEmailParams(to, subject);

        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);

            javaMailSender.send(mail);

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

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);

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


