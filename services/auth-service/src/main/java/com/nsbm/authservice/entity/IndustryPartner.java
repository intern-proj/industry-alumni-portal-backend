package com.nsbm.authservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "industry_partners")
public class IndustryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "representative_full_name", length = 150)
    private String representativeFullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "representative_job_role", length = 100)
    private String representativeJobRole;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "company_industry", length = 100)
    private String companyIndustry;

    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "company_size", length = 100)
    private String companySize;

    @Column(name = "account_status", nullable = false, length = 20)
    @Builder.Default
    private String accountStatus = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
