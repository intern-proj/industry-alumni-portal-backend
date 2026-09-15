package com.nsbm.vacancyservice.dto.request;

import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.entity.VacancyStatus;
import com.nsbm.vacancyservice.entity.WorkplaceType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVacancyRequest {

    private String title;
    private String description;
    private String requirements;
    private String location;
    private JobType jobType;
    private WorkplaceType workplaceType;
    private VacancyStatus status;
    private String salaryRange;
    private LocalDate applicationDeadline;
    private String tags;
    private String targetFaculties;
    private Integer numberOfOpenings;
    private String storageFileId;
    private String aiMissingFields;
}
