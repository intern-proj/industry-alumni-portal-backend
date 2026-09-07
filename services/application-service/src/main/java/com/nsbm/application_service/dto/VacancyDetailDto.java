package com.nsbm.application_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VacancyDetailDto {
    private Long id;
    private String partnerId;
    private String companyName;
    private String title;
    private String description;
    private String requirements;
    private String location;
    private String jobType;
    private String workplaceType;
    private String status;
    private String salaryRange;
    private String tags;
    private String targetFaculties;
}
