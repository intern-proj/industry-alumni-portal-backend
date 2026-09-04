package com.nsbm.eventmanagementservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guest_speakers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestSpeaker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String title;

    private String company;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String email;

    private String phone;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
