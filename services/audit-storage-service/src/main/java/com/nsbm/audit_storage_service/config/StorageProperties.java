package com.nsbm.audit_storage_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String bucketName;
    private String region;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true;
}
