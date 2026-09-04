package com.nsbm.application_service.client;

import com.nsbm.application_service.dto.UserApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    @GetMapping("/api/v1/user-profiles/{userId}")
    UserApiResponseDto getUserProfile(@PathVariable("userId") String userId);
}
