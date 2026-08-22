package com.nsbm.notification_service.service.handlers;

import com.nsbm.notification_service.dto.AnnouncementEmailDTO;
import com.nsbm.notification_service.exception.NotificationValidationException;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementSendingService {

    private final EmailDeliveryService emailDeliveryService;

    /**
     * Processes an announcement and broadcasts it to all recipients.
     *
     * @param dto the announcement data
     * @return true if all emails were sent successfully, false if any failed
     */
    public boolean announcementProcessing(AnnouncementEmailDTO dto) {
        if (dto == null) {
            throw new NotificationValidationException("Announcement payload cannot be null");
        }
        if (dto.getToEmails() == null || dto.getToEmails().isEmpty()) {
            throw new NotificationValidationException("Announcement recipient list cannot be empty");
        }

        log.info("Processing Announcement '{}' for {} recipients",
                dto.getAnnouncementTitle(), dto.getToEmails().size());

        String subject = "[Announcement] " + dto.getAnnouncementTitle();
        String htmlBody = buildAnnouncementHtml(dto);

        boolean allSucceeded = true;

        for (String recipient : dto.getToEmails()) {
            try {
                emailDeliveryService.sendHtmlEmail(recipient, subject, htmlBody);
                log.info("Announcement email sent to {}", recipient);
            } catch (Exception ex) {
                log.error("Failed to send announcement to {}: {}", recipient, ex.getMessage());
                allSucceeded = false;
            }
        }

        return allSucceeded;
    }


    private String buildAnnouncementHtml(AnnouncementEmailDTO dto) {
        String portalUrl = dto.getPortalUrl() != null ? dto.getPortalUrl() : "#";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #1a237e; color: #ffffff; padding: 24px 32px; }
                        .header h1 { margin: 0; font-size: 22px; }
                        .header p { margin: 4px 0 0 0; font-size: 13px; opacity: 0.8; }
                        .body { padding: 32px; color: #333333; line-height: 1.6; }
                        .body h2 { color: #1a237e; font-size: 18px; margin-top: 0; }
                        .cta { display: inline-block; margin-top: 24px; padding: 12px 28px; background-color: #1a237e; color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 14px; }
                        .footer { background-color: #f0f0f0; padding: 16px 32px; font-size: 12px; color: #888888; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📢 Industry Alumni Portal</h1>
                            <p>Official Announcement</p>
                        </div>
                        <div class="body">
                            <h2>%s</h2>
                            <p>%s</p>
                            <a href="%s" class="cta">Visit Portal</a>
                        </div>
                        <div class="footer">
                            <p>This announcement was sent by <strong>%s</strong> via the Industry Alumni Portal.</p>
                            <p>If you believe you received this in error, please contact support.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                dto.getAnnouncementTitle(),
                dto.getAnnouncementBody().replace("\n", "<br>"),
                portalUrl,
                dto.getSenderName()
        );
    }
}
