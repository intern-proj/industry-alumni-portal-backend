package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestSpeakerRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String title;

    private String company;

    private String bio;

    @Email(message = "Must be a valid email")
    private String email;

    private String phone;

    private String photoUrl;

    private Long organizationId;
}
