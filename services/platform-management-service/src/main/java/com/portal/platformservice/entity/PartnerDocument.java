package com.portal.platformservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Reference to a single verification document belonging to one
 * PartnerVerification. File bytes are never stored here or anywhere in
 * this service — storageFileId is a logical reference to the object held
 * by Audit & Storage Service, resolved to a signed download URL via
 * StorageServiceClient when a coordinator opens the detail view.
 *
 * partnerVerificationId is a real foreign key (both tables live in this
 * service's own database), unlike the cross-service references above.
 */
@Entity
@Table(name = "partner_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "verification")
public class PartnerDocument {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_verification_id", nullable = false)
    private PartnerVerification verification;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    /** Logical reference to the file object in Audit & Storage Service — no FK. */
    @Column(name = "storage_file_id", nullable = false)
    private UUID storageFileId;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
