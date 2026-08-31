package com.nsbm.vacancyservice.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VacancyEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:vacancy.exchange}")
    private String vacancyExchange;

    @Value("${app.rabbitmq.notification.exchange:notification.exchange}")
    private String notificationExchange;

    public void publishVacancyDeleted(Long vacancyId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vacancyId", vacancyId);
        
        log.info("Publishing vacancy.deleted event for vacancy ID: {}", vacancyId);
        rabbitTemplate.convertAndSend(vacancyExchange, "vacancy.deleted", payload);
    }

    public void publishVacancyApproved(Long vacancyId, String title, String companyName, String email) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toEmail", email);
        payload.put("recipientName", companyName != null ? companyName : "Corporate Partner");
        payload.put("updateType", "VACANCY_APPROVED");
        payload.put("updateBody", "Your vacancy for '" + title + "' has been approved by the Faculty Coordinator and published live to undergraduate students.\n\nYou can now review applications and discover matching candidates on your dashboard.");
        payload.put("actionLink", "http://localhost:5173/partner/vacancies/" + vacancyId);

        log.info("Publishing VACANCY_APPROVED notification for vacancy ID: {} to {}", vacancyId, email);
        rabbitTemplate.convertAndSend(notificationExchange, "notification.update", payload);
    }

    public void publishVacancyChangesRequested(Long vacancyId, String title, String companyName, String email, String modificationNotes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toEmail", email);
        payload.put("recipientName", companyName != null ? companyName : "Corporate Partner");
        payload.put("updateType", "VACANCY_CHANGES_REQUESTED");
        payload.put("updateBody", "The Faculty Coordinator requested modifications for your vacancy '" + title + "'.\n\nCoordinator Feedback:\n" 
                + (modificationNotes != null ? modificationNotes : "Please review and adjust your submission.") 
                + "\n\nPlease click the button below to edit your job post and resubmit for approval.");
        payload.put("actionLink", "http://localhost:5173/partner/vacancies/" + vacancyId);

        log.info("Publishing VACANCY_CHANGES_REQUESTED notification for vacancy ID: {} to {}", vacancyId, email);
        rabbitTemplate.convertAndSend(notificationExchange, "notification.update", payload);
    }

    public void publishVacancyRejected(Long vacancyId, String title, String companyName, String email, String rejectionReason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toEmail", email);
        payload.put("recipientName", companyName != null ? companyName : "Corporate Partner");
        payload.put("updateType", "VACANCY_REJECTED");
        payload.put("updateBody", "Your vacancy submission for '" + title + "' could not be approved at this time.\n\nReview Notes:\n" 
                + (rejectionReason != null ? rejectionReason : "Does not meet university curriculum or posting criteria."));
        payload.put("actionLink", "http://localhost:5173/partner/vacancies/" + vacancyId);

        log.info("Publishing VACANCY_REJECTED notification for vacancy ID: {} to {}", vacancyId, email);
        rabbitTemplate.convertAndSend(notificationExchange, "notification.update", payload);
    }

    public void publishVacancyFlyerUploaded(Long vacancyId, String partnerId, String storageFileId, String fileUrl) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vacancyId", vacancyId);
        payload.put("partnerId", partnerId);
        payload.put("storageFileId", storageFileId);
        payload.put("fileUrl", fileUrl != null ? fileUrl : "http://localhost:8080/api/v1/storage/download/" + storageFileId);

        log.info("Publishing vacancy.flyer.process event for vacancy ID: {}, storageFileId: {}", vacancyId, storageFileId);
        rabbitTemplate.convertAndSend(vacancyExchange, "vacancy.flyer.process", payload);
    }
}
