package com.portal.platformservice.service;

import com.portal.platformservice.event.VacancyApprovalDecidedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class VacancyApprovalCallbackListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecided(VacancyApprovalDecidedEvent event) {
        // VacancyServiceClient does not exist yet. The record stays at
        // PENDING_CALLBACK; CallbackRetryScheduler will keep finding it
        // until that Feign client is implemented and wired in here.
        log.warn("Vacancy approval {} decided as {}, but VacancyServiceClient is not yet implemented; "
                        + "leaving syncStatus=PENDING_CALLBACK for CallbackRetryScheduler to retry",
                event.approvalId(), event.outcome());
    }
}
