package com.portal.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @Column(name = "resume_id")
    private String resumeId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", length = 50)
    private String fileSize;

    @Column(name = "target_role", length = 100)
    private String targetRole;

    @Column(name = "storage_file_id", length = 100)
    private String storageFileId;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.isPrimary == null) {
            this.isPrimary = false;
        }
    }
}