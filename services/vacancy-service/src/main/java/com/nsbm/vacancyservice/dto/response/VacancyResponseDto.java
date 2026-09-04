package com.nsbm.vacancyservice.dto.response;

import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.entity.VacancyStatus;
import com.nsbm.vacancyservice.entity.WorkplaceType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyResponseDto {

    private Long id;
    private String partnerId;
    private String companyName;
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
    private Integer applicantCount;
    private String rejectionReason;
    private String coordinatorNotes;
    private String aiMissingFields;
    private String storageFileId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
