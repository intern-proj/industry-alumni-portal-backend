package com.nsbm.eventmanagementservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestSpeakerResponse {
    private Long id;
    private String fullName;
    private String title;
    private String company;
    private String bio;
    private String email;
    private String phone;
    private String photoUrl;
    private Long organizationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
