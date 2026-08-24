package com.portal.userprofileservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faculties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faculty {

    @Id
    @Column(name = "faculty_id")
    private String facultyId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "code", unique = true)
    private String code;

    private String description;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Department> departments = new ArrayList<>();
}
