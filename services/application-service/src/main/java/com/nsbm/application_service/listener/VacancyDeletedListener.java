package com.nsbm.application_service.listener;

import com.nsbm.application_service.repository.JobApplicationRepository;
import com.nsbm.application_service.model.JobApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VacancyDeletedListener {

    private final JobApplicationRepository jobApplicationRepository;

    @RabbitListener(queues = "vacancy.deleted.queue")
    public void handleVacancyDeleted(Map<String, Object> payload) {
        try {
            if (payload.containsKey("vacancyId")) {
                Long vacancyId = Long.valueOf(payload.get("vacancyId").toString());
                log.info("Received vacancy.deleted event for vacancy ID: {}. Triggering cascade delete of applications.", vacancyId);
                
                // Assuming jobApplicationRepository has a method to delete by vacancyId
                List<JobApplication> apps = jobApplicationRepository.findByVacancyId(vacancyId);
                jobApplicationRepository.deleteAll(apps);
                
                log.info("Successfully deleted all applications for vacancy ID: {}", vacancyId);
            }
        } catch (Exception ex) {
            log.error("Failed to process vacancy.deleted event: {}", ex.getMessage(), ex);
        }
    }
}
