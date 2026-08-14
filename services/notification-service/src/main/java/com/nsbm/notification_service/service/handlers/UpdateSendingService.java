package com.nsbm.notification_service.service.handlers;

import com.nsbm.notification_service.dto.UpdateEmailDTO;
import com.nsbm.notification_service.exception.NotificationException;
import com.nsbm.notification_service.exception.NotificationValidationException;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateSendingService {

    private final EmailDeliveryService emailDeliveryService;

    /**
     * Processes a portal update notification and sends it to the recipient.
     *
     * @param dto the update data
     * @return true if the email was sent successfully
     */
    public boolean updateProcessing(UpdateEmailDTO dto) {
        if (dto == null) {
            throw new NotificationValidationException("Update payload cannot be null");
        }
        if (dto.getToEmail() == null || dto.getToEmail().isBlank()) {
            throw new NotificationValidationException("Recipient email cannot be null or blank for update notification");
        }

        log.info("Processing {} Update notification for {}", dto.getUpdateType(), dto.getToEmail());

        String subject = buildSubject(dto);
        String htmlBody = buildUpdateHtml(dto);

        try {
            emailDeliveryService.sendHtmlEmail(dto.getToEmail(), subject, htmlBody);
            return true;
        } catch (NotificationException ne) {
            throw ne;
        } catch (Exception ex) {
            throw new NotificationException("Failed to send update email to " + dto.getToEmail(), ex);
        }
    }


    private String buildSubject(UpdateEmailDTO dto) {
        return switch (dto.getUpdateType()) {
            case PROFILE_APPROVED    -> "✅ Your Profile Has Been Approved — Industry Alumni Portal";
            case JOB_POSTED          -> "💼 New Job Opportunity Posted — Industry Alumni Portal";
            case APPLICATION_UPDATE  -> "📋 Application Status Update — Industry Alumni Portal";
            case GENERAL_UPDATE      -> "📬 Portal Update — Industry Alumni Portal";
        };
    }

    private String buildUpdateHtml(UpdateEmailDTO dto) {
        String intro = switch (dto.getUpdateType()) {
            case PROFILE_APPROVED -> "Great news! Your profile on the Industry Alumni Portal has been reviewed and approved.";
            case JOB_POSTED -> "A new job opportunity has been posted on the Industry Alumni Portal that may interest you.";
            case APPLICATION_UPDATE -> "There has been an update regarding one of your applications on the Industry Alumni Portal.";
            case GENERAL_UPDATE -> "You have a new update from the Industry Alumni Portal.";
        };

        String actionBtn = (dto.getActionLink() != null && !dto.getActionLink().isBlank())
                ? "<a href=\"" + dto.getActionLink() + "\" class=\"btn\">View Details</a>"
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #2e7d32; color: #ffffff; padding: 24px 32px; }
                        .header h1 { margin: 0; font-size: 22px; }
                        .body { padding: 32px; color: #333333; line-height: 1.6; }
                        .btn { display: inline-block; margin-top: 20px; padding: 12px 28px; background-color: #2e7d32; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; }
                        .footer { background-color: #f0f0f0; padding: 16px 32px; font-size: 12px; color: #888888; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📬 %s</h1>
                        </div>
                        <div class="body">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>%s</p>
                            <p>%s</p>
                            %s
                        </div>
                        <div class="footer">
                            <p>Industry Alumni Portal Team</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                dto.getUpdateType().name().replace("_", " "),
                dto.getRecipientName(),
                intro,
                dto.getUpdateBody().replace("\n", "<br>"),
                actionBtn
        );
    }
}

