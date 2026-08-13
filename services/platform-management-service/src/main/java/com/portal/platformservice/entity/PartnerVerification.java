package com.portal.platformservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lk.ac.iceu.platformmanagement.common.entity.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Governance record for an industry partner's onboarding approval.
 *
 * This entity deliberately does NOT duplicate the partner's business
 * profile (address, website, industry, logo, etc.) — that data is owned
 * and persisted by User Service. Only the fields needed to run the review
 * workflow are stored here, plus two *_snapshot columns used purely to
 * render the review queue without a Feign call per row. Snapshot columns
 * are write-once at submission time and are never treated as the source
 * of truth for anything beyond that list view.
 *
 * userId, reviewedByUserId are plain UUID columns with no foreign key —
 * each microservice owns a separate database, so no cross-service FK is
 * possible. Referential correctness for these is enforced in the service
 * layer via UserServiceClient, not by the database.
 */
@Entity
@Table(name = "partner_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "documents")
public class PartnerVerification {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Logical reference to the partner's account in User Service — no FK. */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "organization_name_snapshot", length = 255)
    private String organizationNameSnapshot;

    @Column(name = "contact_email_snapshot", length = 255)
    private String contactEmailSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private VerificationStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Logical reference to the coordinator who made the decision — no FK. */
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

    /** Optimistic lock — prevents two coordinators claiming/deciding the same record concurrently. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "verification", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PartnerDocument> documents = new ArrayList<>();

    public void addDocument(PartnerDocument document) {
        documents.add(document);
        document.setVerification(this);
    }

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
        if (status == null) {
            status = VerificationStatus.PENDING_DOCUMENTS;
        }
        if (syncStatus == null) {
            syncStatus = SyncStatus.SYNCED;
        }
    }
}
