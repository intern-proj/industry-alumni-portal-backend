package com.portal.platformservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail entry recording a single status transition on
 * either a PartnerVerification or a VacancyApproval.
 *
 * This table is intentionally polymorphic (approvalType + approvalId)
 * rather than split per workflow, because both workflows record identical
 * information about a status change and a real foreign key cannot span
 * two possible parent tables. Rows are append-only: written once by
 * ApprovalHistoryService and never updated or deleted.
 */
@Entity
@Table(name = "approval_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStatusHistory {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 32)
    private ApprovalType approvalType;

    /** Logical reference to PartnerVerification.id or VacancyApproval.id — no FK, resolved by approvalType. */
    @Column(name = "approval_id", nullable = false)
    private UUID approvalId;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;

    /** Logical reference to the acting user, owned by User Service — no FK. */
    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }
}
