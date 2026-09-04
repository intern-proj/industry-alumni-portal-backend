package com.portal.user_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPreferenceRequestDto {
    private String jobRole;
    private String location;
    private String jobType;
}
