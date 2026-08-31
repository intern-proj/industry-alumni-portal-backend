package com.nsbm.vacancyservice.dto.request;

import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.entity.WorkplaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVacancyRequest {

    @NotNull(message = "Partner ID is required")
    private String partnerId;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String requirements;

    private String location;

    @NotNull(message = "Job type is required (INTERNSHIP, FULL_TIME, etc.)")
    private JobType jobType;

    private WorkplaceType workplaceType;

    private String salaryRange;

    private LocalDate applicationDeadline;

    private String tags;

    private String targetFaculties;

    private Integer numberOfOpenings;

    private String coordinatorNotes;

    private String aiMissingFields;

    private String storageFileId;
}
