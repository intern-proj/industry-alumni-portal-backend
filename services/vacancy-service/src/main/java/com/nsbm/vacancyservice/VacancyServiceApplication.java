package com.nsbm.vacancyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@org.springframework.context.annotation.Import({com.nsbm.common.security.SecurityConfig.class, com.nsbm.common.security.JwtAuthenticationFilter.class, com.nsbm.common.security.JwtTokenProvider.class})
public class VacancyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VacancyServiceApplication.class, args);
    }
}
