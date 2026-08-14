package com.nsbm.notification_service.service.handlers;

import com.nsbm.notification_service.dto.OtpEmailDTO;
import com.nsbm.notification_service.exception.NotificationException;
import com.nsbm.notification_service.exception.NotificationValidationException;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpSendingService {

    private final EmailDeliveryService emailDeliveryService;

    public boolean OtpProcessing(OtpEmailDTO otp) {
        if (otp == null) {
            throw new NotificationValidationException("OTP payload cannot be null");
        }
        if (otp.getToEmail() == null || otp.getToEmail().isBlank()) {
            throw new NotificationValidationException("Target email cannot be null or blank for OTP");
        }
        if (otp.getOtpCode() == null || otp.getOtpCode().isBlank()) {
            throw new NotificationValidationException("OTP code cannot be null or blank");
        }

        log.info("Processing OTP for {}", otp.getToEmail());

        String subject = "OTP Code For Verification";

        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #1a237e; color: #ffffff; padding: 24px 32px; text-align: center; }
                        .header h1 { margin: 0; font-size: 22px; }
                        .body { padding: 32px; color: #333333; line-height: 1.6; }
                        .otp-code { font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1a237e; background: #e8eaf6; padding: 16px; text-align: center; border-radius: 6px; margin: 20px 0; }
                        .footer { background-color: #f0f0f0; padding: 16px 32px; font-size: 12px; color: #888888; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔒 Verification Code</h1>
                        </div>
                        <div class="body">
                            <p>Hello User,</p>
                            <p>Your One-Time Password (OTP) for the Industry Alumni Portal is:</p>
                            <div class="otp-code">%s</div>
                            <p>This code will expire in 10 minutes.</p>
                            <p>If you did not request this code, please ignore this email or contact support immediately.</p>
                        </div>
                        <div class="footer">
                            <p>System Administration — Industry Alumni Portal</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(otp.getOtpCode());

        try {
            emailDeliveryService.sendHtmlEmail(otp.getToEmail(), subject, htmlBody);
            return true;

        } catch (NotificationException ne) {
            throw ne;
        } catch (Exception e) {
            log.error("Unexpected failure sending OTP to {}: {}", otp.getToEmail(), e.getMessage());
            throw new NotificationException("Unexpected error during OTP processing", e);
        }
    }
}

