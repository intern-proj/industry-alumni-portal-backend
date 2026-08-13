package com.nsbm.notification_service.service.handlers;


import com.nsbm.notification_service.dto.OtpEmailDTO;
import com.nsbm.notification_service.service.core.EmailDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpSendingService {

    private final EmailDeliveryService emailDeliveryService;

    public boolean OtpProcessing (OtpEmailDTO otp){
        log.info("Processing OTP for {}",otp.getToEmail());

        String subject = "OTP Code For Verification";

        String body = String.format(
                "Hello User,\n\n" +
                "Your One-Time Password (OTP) for the Industry Alumni Portal is: %s\n\n" +
                "This code will expire in 10 minutes.\n\n" +
                "If you did not request this code, please ignore this email or contact support immediately.\n\n" +
                "Best Regards,\n" +
                "System Administration",
                otp.getOtpCode()
        );

        try {

            emailDeliveryService.sendEmail(otp.getToEmail(),subject,body);

            return true;

        } catch (Exception e) {

            throw new RuntimeException(e);

        }


    }

}
