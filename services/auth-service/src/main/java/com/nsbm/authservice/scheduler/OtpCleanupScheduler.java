package com.nsbm.authservice.scheduler;

import com.nsbm.authservice.repository.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OtpCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpCleanupScheduler.class);
    private final OtpCodeRepository otpCodeRepository;

    public OtpCleanupScheduler(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    /**
     * Permanent deletion of expired OTP codes once a day at 00:00 a.m.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredOtpCodes() {
        log.info("Starting daily cleanup of expired OTP codes...");
        LocalDateTime now = LocalDateTime.now();
        otpCodeRepository.deleteByExpiresAtBefore(now);
        log.info("Expired OTP codes cleanup completed successfully.");
    }
}

