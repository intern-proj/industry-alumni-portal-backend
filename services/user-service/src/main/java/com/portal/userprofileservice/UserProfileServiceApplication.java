package com.portal.userprofileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.context.annotation.Import({com.nsbm.common.security.SecurityConfig.class, com.nsbm.common.security.JwtAuthenticationFilter.class, com.nsbm.common.security.JwtTokenProvider.class})
public class UserProfileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserProfileServiceApplication.class, args);
    }
}