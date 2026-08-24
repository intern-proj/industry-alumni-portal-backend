package com.portal.platformservice.scheduler;

import com.portal.platformservice.entity.PartnerVerification;
import com.portal.platformservice.entity.SyncStatus;
import com.portal.platformservice.entity.VacancyApproval;
import com.portal.platformservice.repository.PartnerVerificationRepository;
import com.portal.platformservice.repository.VacancyApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallbackRetryScheduler {

    private final PartnerVerificationRepository partnerVerificationRepository;
    private final VacancyApprovalRepository vacancyApprovalRepository;

    @Scheduled(fixedDelayString = "${platform.callback-retry.fixed-delay-ms:60000}")
    public void retryPendingCallbacks() {
        retryPendingPartnerVerificationCallbacks();
        retryPendingVacancyApprovalCallbacks();
    }

    private void retryPendingPartnerVerificationCallbacks() {
        List<PartnerVerification> pending = partnerVerificationRepository.findBySyncStatus(SyncStatus.PENDING_CALLBACK);
        if (pending.isEmpty()) {
            return;
        }
        // UserServiceClient does not exist yet, so these can't actually be
        // pushed to User Service. Once that Feign client is added, replay
        // each pending record here and flip syncStatus to SYNCED on success.
        log.warn("{} partner verification(s) awaiting callback to User Service; "
                + "UserServiceClient is not yet implemented", pending.size());
    }

    private void retryPendingVacancyApprovalCallbacks() {
        List<VacancyApproval> pending = vacancyApprovalRepository.findBySyncStatus(SyncStatus.PENDING_CALLBACK);
        if (pending.isEmpty()) {
            return;
        }
        // VacancyServiceClient does not exist yet, so these can't actually
        // be pushed to Vacancy Service. Once that Feign client is added,
        // replay each pending record here and flip syncStatus to SYNCED
        // on success.
        log.warn("{} vacancy approval(s) awaiting callback to Vacancy Service; "
                + "VacancyServiceClient is not yet implemented", pending.size());
    }
}
