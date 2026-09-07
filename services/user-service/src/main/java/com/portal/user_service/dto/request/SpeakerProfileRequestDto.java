package com.portal.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerProfileRequestDto {
    private String userId;

    @NotBlank(message = "Speaker name is required")
    private String name;

    private String organization;
    private String designation;
    private String bio;
    private String contactEmail;
    private String contactPhone;
    private String expertiseTags;
    private String profilePicUrl;
}
