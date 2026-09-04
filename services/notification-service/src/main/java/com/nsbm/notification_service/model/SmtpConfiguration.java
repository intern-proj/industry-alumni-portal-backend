package com.nsbm.notification_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "smtp_configurations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmtpConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    private String username;

    private String password;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "auth_enabled")
    private Boolean authEnabled;

    @Column(name = "starttls_enabled")
    private Boolean starttlsEnabled;

    @Column(name = "ssl_enabled")
    private Boolean sslEnabled;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.authEnabled == null) this.authEnabled = true;
        if (this.starttlsEnabled == null) this.starttlsEnabled = true;
        if (this.sslEnabled == null) this.sslEnabled = false;
        if (this.isActive == null) this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
