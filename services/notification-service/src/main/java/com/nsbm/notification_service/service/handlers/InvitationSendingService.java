package com.nsbm.notification_service.service.handlers;

import com.nsbm.notification_service.dto.InvitationEmailDTO;
import com.nsbm.notification_service.exception.NotificationException;
import com.nsbm.notification_service.exception.NotificationValidationException;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationSendingService {

    private final EmailDeliveryService emailDeliveryService;

    /**
     * Processes an event invitation and sends it to the specified recipient.
     *
     * @param dto the invitation data
     * @return true if the email was sent successfully
     */
    public boolean invitationProcessing(InvitationEmailDTO dto) {
        if (dto == null) {
            throw new NotificationValidationException("Invitation payload cannot be null");
        }
        if (dto.getToEmail() == null || dto.getToEmail().isBlank()) {
            throw new NotificationValidationException("Recipient email cannot be null or blank for invitation");
        }

        log.info("Processing Invitation for '{}' to {}", dto.getEventName(), dto.getToEmail());

        String subject = "You're Invited: " + dto.getEventName();
        String htmlBody = buildInvitationHtml(dto);

        try {
            emailDeliveryService.sendHtmlEmail(dto.getToEmail(), subject, htmlBody);
            return true;
        } catch (NotificationException ne) {
            throw ne;
        } catch (Exception ex) {
            throw new NotificationException("Failed to send invitation email to " + dto.getToEmail(), ex);
        }
    }


    private String buildInvitationHtml(InvitationEmailDTO dto) {
        String rsvpLink = dto.getRsvpLink() != null ? dto.getRsvpLink() : "#";
        String location = dto.getEventLocation() != null ? dto.getEventLocation() : "To be announced";
        String description = dto.getEventDescription() != null
                ? dto.getEventDescription().replace("\n", "<br>")
                : "We look forward to your participation.";
        String organizer = dto.getOrganizerName() != null ? dto.getOrganizerName() : "The Organizing Team";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #0d47a1, #1565c0); color: #ffffff; padding: 32px; text-align: center; }
                        .header h1 { margin: 0 0 8px 0; font-size: 26px; }
                        .header p { margin: 0; font-size: 14px; opacity: 0.85; }
                        .body { padding: 32px; color: #333333; line-height: 1.7; }
                        .greeting { font-size: 16px; margin-bottom: 16px; }
                        .event-card { background: #f0f4ff; border-left: 4px solid #1565c0; border-radius: 6px; padding: 20px; margin: 20px 0; }
                        .event-card .label { font-size: 11px; text-transform: uppercase; color: #1565c0; font-weight: bold; letter-spacing: 0.5px; margin-bottom: 2px; }
                        .event-card .value { font-size: 15px; color: #222; margin-bottom: 12px; }
                        .rsvp-btn { display: inline-block; margin-top: 24px; padding: 14px 36px; background-color: #1565c0; color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 15px; font-weight: bold; }
                        .footer { background-color: #f0f0f0; padding: 16px 32px; font-size: 12px; color: #888888; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 You're Invited!</h1>
                            <p>Industry Alumni Portal — Event Invitation</p>
                        </div>
                        <div class="body">
                            <p class="greeting">Dear <strong>%s</strong>,</p>
                            <p>We are delighted to invite you to the following event:</p>
                            <div class="event-card">
                                <div class="label">Event</div>
                                <div class="value"><strong>%s</strong></div>
                                <div class="label">Date &amp; Time</div>
                                <div class="value">%s</div>
                                <div class="label">Location</div>
                                <div class="value">%s</div>
                                <div class="label">Details</div>
                                <div class="value">%s</div>
                            </div>
                            <a href="%s" class="rsvp-btn">RSVP Now</a>
                        </div>
                        <div class="footer">
                            <p>Organized by <strong>%s</strong> via the Industry Alumni Portal.</p>
                            <p>If you did not expect this invitation, please ignore this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                dto.getInviteeName(),
                dto.getEventName(),
                dto.getEventDate(),
                location,
                description,
                rsvpLink,
                organizer
        );
    }
}
