package com.nsbm.authservice.scheduler;

import com.nsbm.authservice.repository.OtpCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OtpCleanupSchedulerTest {

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @InjectMocks
    private OtpCleanupScheduler otpCleanupScheduler;

    @Test
    @DisplayName("Should invoke repository to delete expired OTP codes")
    void cleanupExpiredOtpCodes_Success() {
        // Act
        otpCleanupScheduler.cleanupExpiredOtpCodes();

        // Assert
        verify(otpCodeRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
}
