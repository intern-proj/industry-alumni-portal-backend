package com.nsbm.application_service.client;

import com.nsbm.application_service.dto.VacancyApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "vacancy-service", url = "${vacancy-service.url:http://localhost:8087}")
public interface VacancyServiceClient {

    @GetMapping("/api/v1/vacancies/public/{id}")
    VacancyApiResponseDto getVacancyById(@PathVariable("id") Long id);
}
