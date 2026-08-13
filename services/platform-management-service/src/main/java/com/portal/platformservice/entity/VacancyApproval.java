package com.portal.platformservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lk.ac.iceu.platformmanagement.common.entity.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Governance record for the review of a single job vacancy submission.
 *
 * Reflects the split-ownership model (Option A): Vacancy Service remains
 * the source of truth for the vacancy's own lifecycle status (what
 * students see). This entity owns only the review workflow — reviewer
 * assignment, decision, reason and its audit trail. On approval or
 * rejection, VacancyServiceClient.pushModeration(...) informs Vacancy
 * Service of the outcome so it can update its own status.
 *
 * NOTE: this table's ownership overlaps with the Services.pdf assignment
 * of vacancy approval to Vacancy Service (see progress report, Risk R1).
 * This entity is built against the split-ownership model pending that
 * decision being confirmed with the team.
 *
 * vacancyId, companyUserId, submittedByUserId, assignedReviewerId and
 * reviewedByUserId are all plain UUID columns with no foreign key — they
 * reference records owned by Vacancy Service or User Service.
 */
@Entity
@Table(name = "vacancy_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyApproval {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Logical reference to the vacancy in Vacancy Service — no FK. */
    @Column(name = "vacancy_id", nullable = false, unique = true)
    private UUID vacancyId;

    /** Logical reference to the posting company's account in User Service — no FK. */
    @Column(name = "company_user_id", nullable = false)
    private UUID companyUserId;

    @Column(name = "submitted_by_user_id")
    private UUID submittedByUserId;

    @Column(name = "vacancy_title_snapshot", length = 255)
    private String vacancyTitleSnapshot;

    @Column(name = "company_name_snapshot", length = 255)
    private String companyNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VacancyApprovalStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "assigned_reviewer_id")
    private UUID assignedReviewerId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "decision_notes", columnDefinition = "TEXT")
    private String decisionNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    /** Optimistic lock — prevents two coordinators deciding the same vacancy concurrently. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
        if (status == null) {
            status = VacancyApprovalStatus.PENDING_REVIEW;
        }
        if (syncStatus == null) {
            syncStatus = SyncStatus.SYNCED;
        }
    }
}
