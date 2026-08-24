package com.nsbm.authservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("Authentication & RBAC governance service for Industry-Alumni Collaboration Portal")
                        .version("1.0.0")
                        .contact(new Contact().name("ICU Platform Team").email("icu-team@nsbm.ac.lk"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
