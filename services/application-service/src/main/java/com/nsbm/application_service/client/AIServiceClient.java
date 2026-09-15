package com.nsbm.application_service.client;

import com.nsbm.application_service.dto.SingleApplicantMatchRequest;
import com.nsbm.application_service.dto.SingleApplicantMatchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", url = "${ai-service.url:http://localhost:8000}")
public interface AIServiceClient {

    @PostMapping("/api/v1/ai/resume/match-single-applicant")
    SingleApplicantMatchResponse computeApplicantMatch(@RequestBody SingleApplicantMatchRequest request);
}
