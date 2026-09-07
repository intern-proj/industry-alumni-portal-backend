package com.portal.user_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @Column(name = "skill_id")
    private String skillId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "skill_level")
    private String skillLevel;

    private String category;
}