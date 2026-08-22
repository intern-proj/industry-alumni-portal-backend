package com.nsbm.audit_storage_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class AuditStorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditStorageServiceApplication.class, args);
    }
}
