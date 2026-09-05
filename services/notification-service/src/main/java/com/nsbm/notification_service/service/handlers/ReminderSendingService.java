package com.nsbm.notification_service.service.handlers;

import com.nsbm.notification_service.dto.ReminderEmailDTO;
import com.nsbm.notification_service.exception.NotificationException;
import com.nsbm.notification_service.exception.NotificationValidationException;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSendingService {

    private final EmailDeliveryService emailDeliveryService;
    private final com.nsbm.notification_service.util.DomainUrlResolver domainUrlResolver;

    /**
     * Processes a reminder notification and sends it to the recipient.
     *
     * @param dto the reminder data
     * @return true if the email was sent successfully
     */
    public boolean reminderProcessing(ReminderEmailDTO dto) {
        if (dto == null) {
            throw new NotificationValidationException("Reminder payload cannot be null");
        }
        if (dto.getToEmail() == null || dto.getToEmail().isBlank()) {
            throw new NotificationValidationException("Recipient email cannot be null or blank for reminder");
        }

        log.info("Processing {} Reminder for {}", dto.getReminderType(), dto.getToEmail());

        String subject = buildSubject(dto);
        String htmlBody = buildReminderHtml(dto);

        try {
            emailDeliveryService.sendHtmlEmail(dto.getToEmail(), subject, htmlBody);
            return true;
        } catch (NotificationException ne) {
            throw ne;
        } catch (Exception ex) {
            throw new NotificationException("Failed to send reminder email to " + dto.getToEmail(), ex);
        }
    }



    private String buildSubject(ReminderEmailDTO dto) {
        return switch (dto.getReminderType()) {
            case EVENT    -> "⏰ Event Reminder: " + dto.getSubject();
            case DEADLINE -> "⚠️ Deadline Reminder: " + dto.getSubject();
            case GENERAL  -> "📌 Reminder: " + dto.getSubject();
        };
    }

    private String buildReminderHtml(ReminderEmailDTO dto) {
        String intro = switch (dto.getReminderType()) {
            case EVENT -> "This is a friendly reminder about an upcoming event.";
            case DEADLINE -> "This is an important reminder that a deadline is approaching.";
            case GENERAL -> "This is a reminder from the Industry Alumni Portal.";
        };

        String dueDateSection = (dto.getDueDate() != null && !dto.getDueDate().isBlank())
                ? "<p><strong>Date / Due Date:</strong> " + dto.getDueDate() + "</p>"
                : "";

        String resolvedActionLink = domainUrlResolver != null
                ? domainUrlResolver.resolveFrontendUrl(dto.getActionLink())
                : dto.getActionLink();

        String actionBtn = (resolvedActionLink != null && !resolvedActionLink.isBlank() && !"#".equals(resolvedActionLink))
                ? "<a href=\"" + resolvedActionLink + "\" class=\"btn\">Take Action</a>"
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #f57c00; color: #ffffff; padding: 24px 32px; }
                        .header h1 { margin: 0; font-size: 22px; }
                        .body { padding: 32px; color: #333333; line-height: 1.6; }
                        .btn { display: inline-block; margin-top: 20px; padding: 12px 28px; background-color: #f57c00; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; }
                        .footer { background-color: #f0f0f0; padding: 16px 32px; font-size: 12px; color: #888888; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>⏰ %s</h1>
                        </div>
                        <div class="body">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>%s</p>
                            <p>%s</p>
                            %s
                            %s
                        </div>
                        <div class="footer">
                            <p>Industry Alumni Portal Team</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                dto.getSubject(),
                dto.getRecipientName(),
                intro,
                dto.getReminderBody().replace("\n", "<br>"),
                dueDateSection,
                actionBtn
        );
    }
}

