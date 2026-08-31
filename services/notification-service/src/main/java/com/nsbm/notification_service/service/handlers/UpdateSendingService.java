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
            case PROFILE_APPROVED          -> "✅ Your Profile Has Been Approved — Industry Alumni Portal";
            case JOB_POSTED                -> "💼 New Job Opportunity Posted — Industry Alumni Portal";
            case APPLICATION_UPDATE        -> "📋 Application Status Update — Industry Alumni Portal";
            case GENERAL_UPDATE            -> "📬 Portal Update — Industry Alumni Portal";
            case VACANCY_APPROVED          -> "✅ Vacancy Approved & Published — Industry Alumni Portal";
            case VACANCY_CHANGES_REQUESTED -> "📝 Action Required: Changes Requested for Vacancy — Industry Alumni Portal";
            case VACANCY_REJECTED          -> "❌ Vacancy Review Notice — Industry Alumni Portal";
        };
    }

    private String buildUpdateHtml(UpdateEmailDTO dto) {
        String headerColor = switch (dto.getUpdateType()) {
            case PROFILE_APPROVED, JOB_POSTED, VACANCY_APPROVED -> "#059669"; // Emerald
            case VACANCY_CHANGES_REQUESTED -> "#4f46e5"; // Indigo
            case VACANCY_REJECTED -> "#e11d48"; // Rose
            default -> "#2e7d32"; // Dark green
        };

        String headerTitle = switch (dto.getUpdateType()) {
            case PROFILE_APPROVED -> "Profile Approved";
            case JOB_POSTED -> "New Opportunity Posted";
            case APPLICATION_UPDATE -> "Application Status Update";
            case GENERAL_UPDATE -> "Portal Notification";
            case VACANCY_APPROVED -> "Vacancy Approved & Published";
            case VACANCY_CHANGES_REQUESTED -> "Modification Requested";
            case VACANCY_REJECTED -> "Vacancy Review Notice";
        };

        String intro = switch (dto.getUpdateType()) {
            case PROFILE_APPROVED -> "Great news! Your profile on the Industry Alumni Portal has been reviewed and approved.";
            case JOB_POSTED -> "A new job opportunity has been posted on the Industry Alumni Portal that may interest you.";
            case APPLICATION_UPDATE -> "There has been an update regarding one of your applications on the Industry Alumni Portal.";
            case GENERAL_UPDATE -> "You have a new update from the Industry Alumni Portal.";
            case VACANCY_APPROVED -> "Great news! Your job vacancy has been reviewed and approved by the Faculty Coordinator and is now live on the portal.";
            case VACANCY_CHANGES_REQUESTED -> "The Faculty Coordinator has reviewed your job vacancy and requested minor modifications before it can be published.";
            case VACANCY_REJECTED -> "Your job vacancy submission has been reviewed by the Faculty Coordinator and could not be approved at this time.";
        };

        String actionBtnText = switch (dto.getUpdateType()) {
            case VACANCY_CHANGES_REQUESTED -> "Edit & Resubmit Vacancy";
            case VACANCY_APPROVED -> "View Live Vacancy";
            default -> "View Details";
        };

        String actionBtn = (dto.getActionLink() != null && !dto.getActionLink().isBlank())
                ? "<a href=\"" + dto.getActionLink() + "\" class=\"btn\" style=\"background-color: " + headerColor + ";\">" + actionBtnText + "</a>"
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e2e8f0; }
                        .header { background-color: %s; color: #ffffff; padding: 28px 32px; }
                        .header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                        .body { padding: 32px; color: #1e293b; line-height: 1.6; font-size: 15px; }
                        .info-box { background-color: #f1f5f9; border-left: 4px solid %s; padding: 16px; border-radius: 6px; margin: 20px 0; color: #334155; font-size: 14px; }
                        .btn { display: inline-block; margin-top: 24px; padding: 12px 28px; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; }
                        .footer { background-color: #f8fafc; padding: 20px 32px; font-size: 12px; color: #64748b; text-align: center; border-top: 1px solid #e2e8f0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="body">
                            <p>Hello <strong>%s</strong>,</p>
                            <p>%s</p>
                            <div class="info-box">
                                %s
                            </div>
                            %s
                        </div>
                        <div class="footer">
                            <p>National Institute of Business Management (NSBM) — Industry Collaboration Unit</p>
                            <p>This is an automated notification. Please do not reply directly to this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                headerColor,
                headerColor,
                headerTitle,
                dto.getRecipientName(),
                intro,
                dto.getUpdateBody().replace("\n", "<br>"),
                actionBtn
        );
    }
}
