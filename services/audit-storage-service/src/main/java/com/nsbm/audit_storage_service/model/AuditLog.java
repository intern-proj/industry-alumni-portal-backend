package com.nsbm.audit_storage_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail entry. Mapped as Hibernate {@link Immutable}
 * so updates and deletes are rejected at the ORM layer.
 */
@Entity
@Immutable
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false, length = 100)
    private String userId;

    @Column(name = "action", nullable = false, updatable = false, length = 255)
    private String action;

    @Column(name = "ip_address", nullable = false, updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "resource_type", updatable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", updatable = false, length = 100)
    private String resourceId;

    @Column(name = "details", updatable = false, columnDefinition = "TEXT")
    private String details;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
