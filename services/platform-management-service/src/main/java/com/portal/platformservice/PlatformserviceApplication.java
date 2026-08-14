package com.portal.platformservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlatformserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlatformserviceApplication.class, args);
	}

}
