package com.nsbm.application_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VacancyApiResponseDto {
    private boolean success;
    private String message;
    private VacancyDetailDto data;
}
