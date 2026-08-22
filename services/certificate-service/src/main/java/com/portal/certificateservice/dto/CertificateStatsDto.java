package com.portal.certificateservice.dto;

public class CertificateStatsDto {

    private long totalCertificatesIssued;
    private long totalActiveTemplates;
    private long totalVerificationsCount;

    public CertificateStatsDto() {
    }

    public CertificateStatsDto(long totalCertificatesIssued, long totalActiveTemplates, long totalVerificationsCount) {
        this.totalCertificatesIssued = totalCertificatesIssued;
        this.totalActiveTemplates = totalActiveTemplates;
        this.totalVerificationsCount = totalVerificationsCount;
    }

    public long getTotalCertificatesIssued() {
        return totalCertificatesIssued;
    }

    public void setTotalCertificatesIssued(long totalCertificatesIssued) {
        this.totalCertificatesIssued = totalCertificatesIssued;
    }

    public long getTotalActiveTemplates() {
        return totalActiveTemplates;
    }

    public void setTotalActiveTemplates(long totalActiveTemplates) {
        this.totalActiveTemplates = totalActiveTemplates;
    }

    public long getTotalVerificationsCount() {
        return totalVerificationsCount;
    }

    public void setTotalVerificationsCount(long totalVerificationsCount) {
        this.totalVerificationsCount = totalVerificationsCount;
    }
}
