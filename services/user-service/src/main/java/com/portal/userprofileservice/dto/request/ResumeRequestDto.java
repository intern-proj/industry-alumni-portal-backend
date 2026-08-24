package com.portal.userprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeRequestDto {
    @NotBlank(message = "File URL is required")
    private String fileUrl;

    @NotBlank(message = "File name is required")
    private String fileName;

    private Boolean isPrimary;
}
