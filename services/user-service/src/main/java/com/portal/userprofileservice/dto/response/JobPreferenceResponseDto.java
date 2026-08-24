package com.portal.userprofileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPreferenceResponseDto {
    private String preferenceId;
    private String userId;
    private String jobRole;
    private String location;
    private String jobType;
}
