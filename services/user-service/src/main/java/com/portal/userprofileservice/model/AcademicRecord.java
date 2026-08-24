package com.portal.userprofileservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "academic_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicRecord {

    @Id
    @Column(name = "record_id")
    private String recordId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    private String faculty;

    private String department;

    @Column(name = "degree_program")
    private String degreeProgram;

    private Integer semester;

    private Integer year;

    private Double gpa;

    private String batch;

    @Column(name = "transcript_url")
    private String transcriptUrl;
}
