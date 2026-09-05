package com.nsbm.notification_service.config;

import com.nsbm.notification_service.model.SmtpConfiguration;
import com.nsbm.notification_service.repository.SmtpConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class SmtpDataInitializer implements CommandLineRunner {

    private final SmtpConfigurationRepository smtpRepository;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private Integer mailPort;

    @Value("${spring.mail.username:otakuslimesgeneration@gmail.com}")
    private String mailUsername;

    @Value("${spring.mail.password:gzes urwv wnpy tixo}")
    private String mailPassword;

    @Override
    public void run(String... args) {
        seedDefaultSmtpConfig();
    }

    public void seedDefaultSmtpConfig() {
        if (smtpRepository.count() == 0) {
            log.info("[SmtpDataInitializer] No active SMTP configuration found. Seeding default configuration...");

            SmtpConfiguration defaultSmtp = SmtpConfiguration.builder()
                    .host(mailHost)
                    .port(mailPort)
                    .username(mailUsername)
                    .password(mailPassword)
                    .senderEmail(mailUsername)
                    .senderName("NSBM Industry & Alumni Portal")
                    .authEnabled(true)
                    .starttlsEnabled(true)
                    .sslEnabled(false)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            smtpRepository.save(defaultSmtp);
            log.info("[SmtpDataInitializer] Default SMTP configuration successfully initialized for host: {}:{}", mailHost, mailPort);
        } else {
            log.info("[SmtpDataInitializer] SMTP configuration already exists (count={}). Skipping seeding.", smtpRepository.count());
        }
    }
}
