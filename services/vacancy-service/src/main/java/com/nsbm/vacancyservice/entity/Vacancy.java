package com.nsbm.vacancyservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies", indexes = {
        @Index(name = "idx_vacancy_status", columnList = "status"),
        @Index(name = "idx_vacancy_partner", columnList = "partner_id"),
        @Index(name = "idx_vacancy_job_type", columnList = "job_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private String partnerId;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "location", length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", length = 30)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "workplace_type", length = 30)
    private WorkplaceType workplaceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VacancyStatus status;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    @Column(name = "tags", length = 300)
    private String tags;

    @Column(name = "storage_file_id", length = 100)
    private String storageFileId;

    @Column(name = "ai_missing_fields", columnDefinition = "TEXT")
    private String aiMissingFields;

    @Column(name = "target_faculties", length = 200)
    private String targetFaculties;

    @Column(name = "number_of_openings")
    private Integer numberOfOpenings;

    @Column(name = "applicant_count")
    @Builder.Default
    private Integer applicantCount = 0;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "coordinator_notes", length = 500)
    private String coordinatorNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
