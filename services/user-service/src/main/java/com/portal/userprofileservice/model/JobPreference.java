package com.portal.userprofileservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPreference {

    @Id
    @Column(name = "preference_id")
    private String preferenceId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "job_role")
    private String jobRole;

    private String location;

    @Column(name = "job_type")
    private String jobType;
}
