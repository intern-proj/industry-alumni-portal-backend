package com.nsbm.notification_service.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailDeliveryService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final JavaMailSender javaMailSender;

    public void sendEmail(String to, String subject, String body){

        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);

            javaMailSender.send(mail);

            log.info("Email sent successfully to - {}",to);

        }catch (Exception ex){
            log.error("Send an Email to {} Failed Because {}",to,ex.getMessage());
            throw new RuntimeException("Email Service Failed",ex);
        }

    }

}
