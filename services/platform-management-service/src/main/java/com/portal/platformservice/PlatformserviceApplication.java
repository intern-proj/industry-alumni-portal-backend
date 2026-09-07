package com.portal.platformservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@org.springframework.context.annotation.Import({com.nsbm.common.security.SecurityConfig.class, com.nsbm.common.security.JwtAuthenticationFilter.class, com.nsbm.common.security.JwtTokenProvider.class})
@EnableScheduling
public class PlatformserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlatformserviceApplication.class, args);
	}

}
