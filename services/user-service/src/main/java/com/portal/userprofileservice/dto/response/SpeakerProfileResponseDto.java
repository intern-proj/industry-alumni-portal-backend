package com.portal.userprofileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerProfileResponseDto {
    private String speakerId;
    private String userId;
    private String name;
    private String organization;
    private String designation;
    private String bio;
    private String contactEmail;
    private String contactPhone;
    private String expertiseTags;
    private String profilePicUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
