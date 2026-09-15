package com.portal.event_participation_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_id", updatable = false, nullable = false)
    private UUID feedbackId;

    @Column(name = "registration_id", nullable = false, unique = true)
    private UUID registrationId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comments", length = 1000)
    private String comments;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false, nullable = false)
    private Instant submittedAt;
}