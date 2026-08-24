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
public class ResumeResponseDto {
    private String resumeId;
    private String userId;
    private String fileUrl;
    private String fileName;
    private Boolean isPrimary;
    private LocalDateTime uploadedAt;
}
